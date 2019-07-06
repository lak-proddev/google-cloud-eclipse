/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.libraries;

import static org.junit.Assert.fail;

import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import com.google.cloud.tools.eclipse.appengine.libraries.model.LibraryFile;
import com.google.cloud.tools.eclipse.test.util.project.TestProjectCreator;
import com.google.cloud.tools.eclipse.util.ArtifactRetriever;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.xml.sax.SAXException;

public class POMFormatTest {

  @Rule
  public final TestProjectCreator projectCreator = new TestProjectCreator();
  private Pom pom;
  private IFile pomFile;

  @Before
  public void setUp() throws SAXException, IOException, CoreException {
    IProject project = projectCreator.getProject();
    pomFile = project.getFile("pom.xml");
    try (InputStream in =
        Files.newInputStream(Paths.get("testdata/testPomEmpty.xml").toAbsolutePath())) {
      pomFile.create(in, IFile.FORCE, null);
      pom = Pom.parse(pomFile);
    }

    Logger logger = Logger.getLogger(ArtifactRetriever.class.getName());
    logger.setLevel(Level.OFF);
  }


  @Test
  public void testAddDependencies() throws Exception {
    Library library0 = PomTest.newLibrary("id0",
        new LibraryFile(PomTest.coordinates("com.example.group0", "artifact0", "1.2.3")));
    Library library1 = PomTest.newLibrary("id1",
        new LibraryFile(PomTest.coordinates("com.example.group1", "artifact1")));
    Library library2 = PomTest.newLibrary("id2",
        new LibraryFile(PomTest.coordinates("com.example.group2", "artifact2")),
        new LibraryFile(PomTest.coordinates("com.example.group3", "artifact3")));

    List<Library> libraries = Arrays.asList(library0, library1, library2);
    pom.addDependencies(libraries);

    Bundle bundle = Platform.getBundle("com.google.cloud.tools.eclipse.appengine.libraries.test");
    URL fileURL = bundle.getEntry("/testdata/formatAdd.xml");
    File expected = new File(FileLocator.resolve(fileURL).toURI());
    assertFileContentsEqual(pomFile.getLocation().toFile(), expected);

  }

  @Test
  public void testRemoveUnusedDependencies() throws Exception {
    LibraryFile file1 = new LibraryFile(PomTest.coordinates("com.example.group1", "artifact1"));
    LibraryFile file2 = new LibraryFile(PomTest.coordinates("com.example.group2", "artifact2"));
    Library library1 = PomTest.newLibrary("id1", file1);
    Library library2 = PomTest.newLibrary("id2", file1, file2);

    pom.addDependencies(Arrays.asList(library1, library2));

    Bundle bundle = Platform.getBundle("com.google.cloud.tools.eclipse.appengine.libraries.test");
    URL fileURL = bundle.getEntry("/testdata/formatRemove.xml");
    File expected = new File(FileLocator.resolve(fileURL).toURI());
    assertFileContentsEqual(pomFile.getLocation().toFile(), expected);
  }

  public static void assertFileContentsEqual(File actual, File expected) throws IOException {
    boolean areEqual = true;
    try (BufferedReader reader1 = new BufferedReader(new FileReader(actual));
        BufferedReader reader2 = new BufferedReader(new FileReader(expected));) {
      String line1 = reader1.readLine();
      String line2 = reader2.readLine();

      int lineNum = 1;

      while (line1 != null || line2 != null) {
        if (line1 == null || line2 == null) {
          areEqual = false;
          break;
        } else if (!line1.equalsIgnoreCase(line2)) {
          areEqual = false;
          break;
        }
        line1 = reader1.readLine();
        line2 = reader2.readLine();
        lineNum++;
      }
      if (!areEqual) {
        String message = "Two files have different content. They differ at line " + lineNum + " \""
            + actual.getName() + "\" has " + line1 + " and \"" + expected.getName() + "\" has "
            + line2 + " at line " + lineNum + "\n";
        fail(message);
      }
    }
  }
}
