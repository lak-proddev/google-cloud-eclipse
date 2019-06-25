/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.cloud.tools.eclipse.appengine.libraries.BuildPath;
import com.google.cloud.tools.eclipse.appengine.libraries.model.CloudLibraries;
import com.google.cloud.tools.eclipse.appengine.libraries.model.Library;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectEvent.Type;
import org.eclipse.wst.common.project.facet.core.events.IFacetedProjectListener;
import org.eclipse.wst.common.project.facet.core.events.IProjectFacetActionEvent;

public class MavenFacetPostInstallChangeListener implements IFacetedProjectListener {
  private static final Logger logger =
      Logger.getLogger(MavenFacetPostInstallChangeListener.class.getName());

  @Override
  public void handleEvent(IFacetedProjectEvent event) {
    try {
      if (!canHandleEvent(event)) {
        return;
      }
      IProjectFacet projectFacet = ((IProjectFacetActionEvent) event).getProjectFacet();
      Collection<Library> availableLibraries = getLibrariesByFacet(projectFacet);
      if (!availableLibraries.isEmpty()) {
        Collection<Library> addedLibraries =
            event.getType() != Type.POST_UNINSTALL ? availableLibraries : Collections.emptyList();
        Collection<Library> removedLibraries =
            event.getType() != Type.POST_UNINSTALL ? Collections.emptyList() : availableLibraries;
        BuildPath.updateMavenLibraries(event.getProject().getProject(), addedLibraries,
            removedLibraries, new NullProgressMonitor());
      }
    } catch (CoreException ex) {
      logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  private boolean canHandleEvent(IFacetedProjectEvent event) {
    return AppEngineStandardFacet.FACET.equals(((IProjectFacetActionEvent) event).getProjectFacet())
        && (event.getType() == Type.POST_INSTALL || event.getType() == Type.POST_UNINSTALL);
  }

  private Collection<Library> getLibrariesByFacet(IProjectFacet projectFacet) {
    String group = "";
    if (AppEngineStandardFacet.FACET.equals(projectFacet)) {
      group = CloudLibraries.APP_ENGINE_STANDARD_GROUP;
    } // In future, we can add for other facets
    return !group.isEmpty() ? getLibraries(group) : Collections.emptyList();
  }

  /**
   * Returns the libraries and required dependencies.
   */
  private Collection<Library> getLibraries(String group) {
    Set<Library> allLibraries = new HashSet<>();
    List<Library> libraries = CloudLibraries.getLibraries(group);
    allLibraries.addAll(libraries);
    addDependencies(libraries, allLibraries);
    return allLibraries;
  }

  private void addDependencies(List<Library> libraries, Set<Library> allLibraries) {
    for (Library library : libraries) {
      for (String dependencyId : library.getLibraryDependencies()) {
        Library dependency = CloudLibraries.getLibrary(dependencyId);
        if (dependency != null) {
          allLibraries.add(dependency);
        }
      }
    }
  }
}
