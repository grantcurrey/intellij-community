/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler.server;

import com.intellij.notification.Notification;
import com.intellij.openapi.compiler.CompilationStatusListener;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.CompilerTopics;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import org.jetbrains.jps.api.CmdlineRemoteProto;

import java.util.Collections;
import java.util.UUID;

/**
* @author Eugene Zhuravlev
*         Date: 4/25/12
*/
class AutoMakeMessageHandler extends DefaultMessageHandler {
  private static final Key<Notification> LAST_AUTO_MAKE_NOFITICATION = Key.create("LAST_AUTO_MAKE_NOFITICATION");
  private CmdlineRemoteProto.Message.BuilderMessage.BuildEvent.Status myBuildStatus;
  private final Project myProject;
  private final WolfTheProblemSolver myWolf;

  public AutoMakeMessageHandler(Project project) {
    super(project);
    myProject = project;
    myBuildStatus = CmdlineRemoteProto.Message.BuilderMessage.BuildEvent.Status.SUCCESS;
    myWolf = WolfTheProblemSolver.getInstance(project);
  }

  @Override
  protected void handleBuildEvent(CmdlineRemoteProto.Message.BuilderMessage.BuildEvent event) {
    if (myProject.isDisposed()) {
      return;
    }
    switch (event.getEventType()) {
      case BUILD_COMPLETED:
        if (event.hasCompletionStatus()) {
          myBuildStatus = event.getCompletionStatus();
        }
        return;

      case FILES_GENERATED:
        final CompilationStatusListener publisher = myProject.getMessageBus().syncPublisher(CompilerTopics.COMPILATION_STATUS);
        for (CmdlineRemoteProto.Message.BuilderMessage.BuildEvent.GeneratedFile generatedFile : event.getGeneratedFilesList()) {
          final String root = FileUtil.toSystemIndependentName(generatedFile.getOutputRoot());
          final String relativePath = FileUtil.toSystemIndependentName(generatedFile.getRelativePath());
          publisher.fileGenerated(root, relativePath);
        }
        return;

      default:
        return;
    }
  }

  @Override
  protected void handleCompileMessage(CmdlineRemoteProto.Message.BuilderMessage.CompileMessage message) {
    if (myProject.isDisposed()) {
      return;
    }
    final CmdlineRemoteProto.Message.BuilderMessage.CompileMessage.Kind kind = message.getKind();
    if (kind == CmdlineRemoteProto.Message.BuilderMessage.CompileMessage.Kind.ERROR) {
      informWolf(myProject, message);
    }
  }

  @Override
  public void handleFailure(UUID sessionId, CmdlineRemoteProto.Message.Failure failure) {
    CompilerManager.NOTIFICATION_GROUP.createNotification("Auto make failure: " + failure.getDescription(), MessageType.INFO);
  }

  @Override
  public void sessionTerminated() {
    String statusMessage = null/*"Auto make completed"*/;
    switch (myBuildStatus) {
      case SUCCESS:
        //statusMessage = "Auto make completed successfully";
        break;
      case UP_TO_DATE:
        //statusMessage = "All files are up-to-date";
        break;
      case ERRORS:
        statusMessage = "Auto make completed with errors";
        break;
      case CANCELED:
        //statusMessage = "Auto make has been canceled";
        break;
    }
    if (statusMessage != null) {
      final Notification notification = CompilerManager.NOTIFICATION_GROUP.createNotification(statusMessage, MessageType.INFO);
      if (!myProject.isDisposed()) {
        notification.notify(myProject);
      }
      myProject.putUserData(LAST_AUTO_MAKE_NOFITICATION, notification);
    } else {
      Notification notification = myProject.getUserData(LAST_AUTO_MAKE_NOFITICATION);
      if (notification != null) {
        notification.expire();
        myProject.putUserData(LAST_AUTO_MAKE_NOFITICATION, null);
      }
    }
  }

  private void informWolf(Project project, CmdlineRemoteProto.Message.BuilderMessage.CompileMessage message) {
    final String srcPath = message.getSourceFilePath();
    if (srcPath != null && !project.isDisposed()) {
      final VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(srcPath);
      if (vFile != null) {
        final int line = (int)message.getLine();
        final int column = (int)message.getColumn();
        if (line > 0 && column > 0) {
          final Problem problem = myWolf.convertToProblem(vFile, line, column, new String[]{message.getText()});
          myWolf.weHaveGotProblems(vFile, Collections.singletonList(problem));
        }
        else {
          myWolf.queue(vFile);
        }
      }
    }
  }
}
