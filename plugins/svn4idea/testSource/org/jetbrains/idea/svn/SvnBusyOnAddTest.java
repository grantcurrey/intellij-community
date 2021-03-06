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
package org.jetbrains.idea.svn;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.PluginPathManager;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.internal.wc17.db.ISVNWCDb;
import org.tmatesoft.svn.core.internal.wc17.db.SVNWCDb;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * Date: 8/7/12
 * Time: 7:15 PM
 */
public class SvnBusyOnAddTest extends TestCase {
  public static final String filename = "test.txt";

  private File myWorkingCopyRoot;

  @Before
  public void setUp() throws Exception {
    //PlatformTestCase.initPlatformLangPrefix();
    File pluginRoot = new File(PluginPathManager.getPluginHomePath("svn4idea"));
    if (!pluginRoot.isDirectory()) {
      // try standalone mode
      Class aClass = Svn17TestCase.class;
      String rootPath = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
      pluginRoot = new File(rootPath).getParentFile().getParentFile().getParentFile();
    }
    myWorkingCopyRoot = new File(pluginRoot, "testData/move2unv");
  }

  @Override
  public void tearDown() throws Exception {
  }

  @Test
  public void testRefusedAdd ()throws Exception {
    SVNWCDb db = new SVNWCDb();
    final File ioFile = new File(myWorkingCopyRoot, filename);
    ioFile.createNewFile();
    try {
      db.open(ISVNWCDb.SVNWCDbOpenMode.ReadWrite, new DefaultSVNOptions(), true, true);
      SVNWCContext context = new SVNWCContext(db, new ISVNEventHandler() {
        @Override
        public void handleEvent(SVNEvent event, double progress) throws SVNException {
        }

        @Override
        public void checkCancelled() throws SVNCancelException {
        }
      });

      File file = context.acquireWriteLock(myWorkingCopyRoot, false, true);
      boolean failed = false;
      try {
        SVNWCClient client = new SVNWCClient((ISVNRepositoryPool)null, new DefaultSVNOptions());
        client.doAdd(ioFile, true, false, false, true);
      }
      catch (SVNException e) {
        Assert.assertEquals(155004, e.getErrorMessage().getErrorCode().getCode());
        failed = true;
      }
      finally {
        context.releaseWriteLock(myWorkingCopyRoot);
      }
      Assert.assertTrue(failed);

      SVNStatusClient readClient = new SVNStatusClient((ISVNRepositoryPool)null, new DefaultSVNOptions());
      readClient.doStatus(ioFile, false);
    }
    finally {
      ioFile.delete();
      db.close();
    }
  }

  @Test
  public void testRefusedAddVariant ()throws Exception {
    SVNWCDb db = new SVNWCDb();
    final File ioFile = new File(myWorkingCopyRoot, filename);
    ioFile.createNewFile();
    SVNWCContext context = null;
    try {
      db.open(ISVNWCDb.SVNWCDbOpenMode.ReadWrite, new DefaultSVNOptions(), true, true);
      context = new SVNWCContext(db, new ISVNEventHandler() {
        @Override
        public void handleEvent(SVNEvent event, double progress) throws SVNException {
        }

        @Override
        public void checkCancelled() throws SVNCancelException {
        }
      });

      File file = context.acquireWriteLock(myWorkingCopyRoot, false, true);
      boolean failed = false;
      try {
        SVNWCClient client = new SVNWCClient((ISVNRepositoryPool)null, new DefaultSVNOptions());
        client.doAdd(ioFile, true, false, false, true);
      }
      catch (SVNException e) {
        Assert.assertEquals(155004, e.getErrorMessage().getErrorCode().getCode());
        failed = true;
      }
      Assert.assertTrue(failed);

      SVNStatusClient readClient = new SVNStatusClient((ISVNRepositoryPool)null, new DefaultSVNOptions());
      readClient.doStatus(ioFile, false);
    }
    finally {
      if (context != null) {
        context.releaseWriteLock(myWorkingCopyRoot);
      }
      ioFile.delete();
      db.close();
    }
  }
}
