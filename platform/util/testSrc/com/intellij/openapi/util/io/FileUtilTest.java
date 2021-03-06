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
package com.intellij.openapi.util.io;

import com.intellij.openapi.util.SystemInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Roman Shevchenko
 * @since 12.04.2012
 */
public class FileUtilTest {
  private static final char UNIX_SEPARATOR = '/';
  private static final char WINDOWS_SEPARATOR = '\\';

  @Test
  public void toCanonicalPath() {
    assertEquals("", FileUtil.toCanonicalPath(""));
    assertEquals("  ", FileUtil.toCanonicalPath("  "));

    assertEquals("/", FileUtil.toCanonicalPath("/", UNIX_SEPARATOR));
    assertEquals("a/b", FileUtil.toCanonicalPath("a/b/", UNIX_SEPARATOR));
    assertEquals("/a/b", FileUtil.toCanonicalPath("/a/////b/", UNIX_SEPARATOR));
    assertEquals("/a/b", FileUtil.toCanonicalPath("/a/././b/", UNIX_SEPARATOR));
    assertEquals("/c", FileUtil.toCanonicalPath("/a/b/../../c", UNIX_SEPARATOR));
    assertEquals("/a", FileUtil.toCanonicalPath("/a/b/..", UNIX_SEPARATOR));
    assertEquals("/a", FileUtil.toCanonicalPath("/a/b/../", UNIX_SEPARATOR));
    assertEquals("/a\\b", FileUtil.toCanonicalPath("/a\\b/", UNIX_SEPARATOR));
    assertEquals("/", FileUtil.toCanonicalPath("/a/../", UNIX_SEPARATOR));
    assertEquals("/", FileUtil.toCanonicalPath("/a/../..", UNIX_SEPARATOR));
    assertEquals("/b", FileUtil.toCanonicalPath("/a/../../b", UNIX_SEPARATOR));
    assertEquals("a...b", FileUtil.toCanonicalPath("a...b", UNIX_SEPARATOR));
    assertEquals("a../b", FileUtil.toCanonicalPath("a../b", UNIX_SEPARATOR));
    assertEquals("a./.b", FileUtil.toCanonicalPath("a./.b", UNIX_SEPARATOR));

    assertEquals("C:/", FileUtil.toCanonicalPath("C:\\", WINDOWS_SEPARATOR));
    assertEquals("a/b", FileUtil.toCanonicalPath("a\\b\\", WINDOWS_SEPARATOR));
    assertEquals("c:/a/b", FileUtil.toCanonicalPath("c:\\a\\\\b\\", WINDOWS_SEPARATOR));
    assertEquals("c:/a/b", FileUtil.toCanonicalPath("c:\\a\\.\\.\\b\\", WINDOWS_SEPARATOR));
    assertEquals("c:/d", FileUtil.toCanonicalPath("c:\\a\\b\\..\\..\\d", WINDOWS_SEPARATOR));
    assertEquals("/a/b", FileUtil.toCanonicalPath("\\a/b\\", WINDOWS_SEPARATOR));
    assertEquals("c:/", FileUtil.toCanonicalPath("c:\\a\\..\\", WINDOWS_SEPARATOR));
    assertEquals("c:/", FileUtil.toCanonicalPath("c:\\a\\..\\..", WINDOWS_SEPARATOR));
    assertEquals("c:/b", FileUtil.toCanonicalPath("c:\\a\\..\\..\\b", WINDOWS_SEPARATOR));
  }

  @Test
  public void isAncestor() throws Exception {
    assertTrue(FileUtil.isAncestor("/a/b/c", "/a/b/c/d/e/f", true));
    assertTrue(FileUtil.isAncestor("/a/b/c/", "/a/b/c/d/e/f", true));
    assertFalse(FileUtil.isAncestor("/a/b/c/1", "/a/b/c/2", true));
    assertFalse(FileUtil.isAncestor("/a/b/c/1", "/a/b/c/2", false));
    assertTrue(FileUtil.isAncestor("/a/b/c/", "/a/b/c", false));
    assertTrue(FileUtil.isAncestor("/a///b/c", "/a/b/c/", false));
    assertFalse(FileUtil.isAncestor("/a/b/c/", "/a/./b/c", true));
    assertFalse(FileUtil.isAncestor("/a/b/c", "/a/b/c/", true));
    assertFalse(FileUtil.isAncestor("/a/b/c", "/a/b/cde", true));
    assertFalse(FileUtil.isAncestor("/a/b/c", "/a/b/cde", false));

    assertEquals(!SystemInfo.isFileSystemCaseSensitive, FileUtil.isAncestor("/a/b/c", "/a/B/c/d", true));
  }
}
