/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.guvnor.ala.ui.client.wizard.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.guvnor.ala.ui.client.util.ContentChangeHandler;
import org.guvnor.ala.ui.client.widget.FormStatus;
import org.guvnor.ala.ui.client.wizard.NewDeployWizard;
import org.guvnor.ala.ui.service.SourceService;
import org.guvnor.common.services.project.model.Module;
import org.jboss.errai.common.client.api.Caller;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.uberfire.mocks.CallerMock;
import org.uberfire.spaces.Space;

import static org.guvnor.ala.ui.client.util.UIUtil.EMPTY_STRING;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(GwtMockitoTestRunner.class)
public class SourceConfigurationParamsPresenterTest {

    private static final int ELEMENTS_SIZE = 5;

    private static final String SOME_VALUE = "SOME_VALUE";

    private static final String RUNTIME_NAME = "RUNTIME_NAME";

    private static final String OU = "OU";

    private static final String REPOSITORY = "REPOSITORY";

    private static final String BRANCH = "BRANCH";

    private static final String MODULE = "PROJECT";

    private static final Space SPACE = new Space(OU);

    @Mock
    private SourceConfigurationParamsPresenter.View view;

    @Mock
    private SourceService sourceService;

    private Caller<SourceService> sourceServiceCaller;

    @Mock
    private ContentChangeHandler changeHandler;

    private SourceConfigurationParamsPresenter presenter;

    private List<String> ous;

    private List<String> repositories;

    private List<String> branches;

    private List<Module> modules;

    @Before
    public void setUp() {
        ous = createOUs();
        repositories = createRepositories();
        branches = createBranches();
        modules = createModules();

        sourceServiceCaller = spy(new CallerMock<>(sourceService));
        presenter = new SourceConfigurationParamsPresenter(view,
                                                           sourceServiceCaller);
        presenter.addContentChangeHandler(changeHandler);
        presenter.init();
        verify(view,
               times(1)).init(presenter);
    }

    @Test
    public void testInitialize() {
        when(sourceService.getOrganizationUnits()).thenReturn(ous);
        presenter.initialise();
        ous.forEach(ou -> verify(view,
                                 times(1)).addOrganizationUnit(ou));
        verify(view,
               times(1)).clearOrganizationUnits();
        verify(view,
               times(1)).clearRepositories();
        verify(view,
               times(1)).clearBranches();
        verify(view,
               times(1)).clearModules();
    }

    @Test
    public void testIsComplete() {
        when(view.getRuntimeName()).thenReturn(EMPTY_STRING);
        when(view.getOU()).thenReturn(EMPTY_STRING);
        when(view.getRepository()).thenReturn(EMPTY_STRING);
        when(view.getBranch()).thenReturn(EMPTY_STRING);
        when(view.getModule()).thenReturn(EMPTY_STRING);

        presenter.isComplete(Assert::assertFalse);

        when(view.getRuntimeName()).thenReturn(SOME_VALUE);
        presenter.isComplete(Assert::assertFalse);

        when(view.getOU()).thenReturn(OU);
        presenter.isComplete(Assert::assertFalse);

        when(view.getRepository()).thenReturn(REPOSITORY);
        presenter.isComplete(Assert::assertFalse);

        //now the branch is completed and emulate the projects are loaded.
        when(view.getBranch()).thenReturn(BRANCH);
        when(sourceService.getModules(eq(SPACE),
                                      eq(REPOSITORY),
                                      eq(BRANCH))).thenReturn(modules);

        presenter.onBranchChange();

        //pick an arbitrary project as the selected one
        int selectedModule = 1;
        String moduleName = modules.get(selectedModule).getModuleName();
        when(view.getModule()).thenReturn(moduleName);
        //completed when al values are in place.
        presenter.isComplete(Assert::assertTrue);
    }

    @Test
    public void testBuildParams() {
        //emulate the page is completed and that there is a selected project.
        when(view.getRuntimeName()).thenReturn(RUNTIME_NAME);
        when(view.getOU()).thenReturn(OU);
        when(view.getRepository()).thenReturn(REPOSITORY);
        when(view.getBranch()).thenReturn(BRANCH);
        when(sourceService.getModules(eq(SPACE),
                                      eq(REPOSITORY),
                                      eq(BRANCH))).thenReturn(modules);
        when(view.getModule()).thenReturn(MODULE);
        presenter.onBranchChange();

        //pick an arbitrary project as the selected one
        int selectedModule = 2;
        String moduleName = modules.get(selectedModule).getModuleName();
        when(view.getModule()).thenReturn(moduleName);

        Map<String, String> params = presenter.buildParams();
        assertEquals(RUNTIME_NAME,
                     params.get(NewDeployWizard.RUNTIME_NAME));
        assertEquals(OU + "/" + REPOSITORY,
                     params.get(SourceConfigurationParamsPresenter.REPO_NAME));
        assertEquals(BRANCH,
                     params.get(SourceConfigurationParamsPresenter.BRANCH));
        assertEquals(moduleName,
                     params.get(SourceConfigurationParamsPresenter.MODULE_DIR));
    }

    @Test
    public void testClear() {
        presenter.clear();
        verify(view,
               times(1)).clear();
    }

    @Test
    public void testDisable() {
        presenter.disable();
        verify(view,
               times(1)).disable();
    }

    @Test
    public void testOnRuntimeChangeValid() {
        when(view.getRuntimeName()).thenReturn(RUNTIME_NAME);
        presenter.onRuntimeNameChange();
        verify(view,
               times(1)).setRuntimeStatus(FormStatus.VALID);
        verifyHandlerNotified();
    }

    @Test
    public void testOnRuntimeChangeInvalid() {
        when(view.getRuntimeName()).thenReturn(EMPTY_STRING);
        presenter.onRuntimeNameChange();
        verify(view,
               times(1)).setRuntimeStatus(FormStatus.ERROR);
        verifyHandlerNotified();
    }

    @Test
    public void testOnOrganizationalUnitChangeValid() {
        when(view.getOU()).thenReturn(OU);
        when(sourceService.getRepositories(OU)).thenReturn(repositories);
        presenter.onOrganizationalUnitChange();

        verify(view,
               times(1)).setOUStatus(FormStatus.VALID);

        verify(view,
               times(2)).getOU();
        verify(view,
               times(2)).clearRepositories();
        verify(view,
               times(2)).clearBranches();
        verify(view,
               times(2)).clearModules();

        verityRepositoriesWereLoaded();
        verifyHandlerNotified();
    }

    @Test
    public void testOnOrganizationalUnitChangeInvalid() {
        when(view.getOU()).thenReturn(EMPTY_STRING);
        presenter.onOrganizationalUnitChange();
        verify(view,
               times(1)).setOUStatus(FormStatus.ERROR);
        verifyHandlerNotified();
    }

    @Test
    public void testOnRepositoryChangeValid() {
        when(view.getRepository()).thenReturn(REPOSITORY);
        when(view.getOU()).thenReturn(OU);
        when(sourceService.getBranches(eq(SPACE), eq(REPOSITORY))).thenReturn(branches);
        presenter.onRepositoryChange();

        verify(view,
               times(1)).setRepositoryStatus(FormStatus.VALID);

        verify(view,
               times(2)).clearBranches();
        verify(view,
               times(2)).clearModules();

        verifyBranchesWereLoaded();
        verifyHandlerNotified();
    }

    @Test
    public void testOnRepositoryChangeInvalid() {
        when(view.getRepository()).thenReturn(EMPTY_STRING);
        presenter.onRepositoryChange();
        verify(view,
               times(1)).setRepositoryStatus(FormStatus.ERROR);
        verifyHandlerNotified();
    }

    @Test
    public void testOnBranchChangeValid() {
        when(view.getOU()).thenReturn(OU);
        when(view.getRepository()).thenReturn(REPOSITORY);
        when(view.getBranch()).thenReturn(BRANCH);
        when(sourceService.getModules(eq(SPACE),
                                      eq(REPOSITORY),
                                      eq(BRANCH))).thenReturn(modules);

        presenter.onBranchChange();

        verify(view,
               times(1)).setBranchStatus(FormStatus.VALID);

        verify(view,
               times(2)).clearModules();

        verifyProjectsWereLoaded();
        verifyHandlerNotified();
    }

    @Test
    public void testOnBranchChangeInvalid() {
        when(view.getBranch()).thenReturn(EMPTY_STRING);
        presenter.onBranchChange();
        verify(view,
               times(1)).setBranchStatus(FormStatus.ERROR);
        verifyHandlerNotified();
    }

    @Test
    public void testOnProjectChangeValid() {
        when(view.getModule()).thenReturn(MODULE);
        presenter.onModuleChange();
        verify(view,
               times(1)).setProjectStatus(FormStatus.VALID);
        verifyHandlerNotified();
    }

    @Test
    public void testOnProjectChangeInValid() {
        when(view.getModule()).thenReturn(EMPTY_STRING);
        presenter.onModuleChange();
        verify(view,
               times(1)).setProjectStatus(FormStatus.ERROR);
        verifyHandlerNotified();
    }

    private void verityRepositoriesWereLoaded() {
        repositories.forEach(repository -> verify(view,
                                                  times(1)).addRepository(repository));
    }

    private void verifyBranchesWereLoaded() {
        branches.forEach(branch -> verify(view,
                                          times(1)).addBranch(branch));
    }

    private void verifyProjectsWereLoaded() {
        modules.forEach(module -> verify(view,
                                         times(1)).addModule(module.getModuleName()));
    }

    private List<String> createOUs() {
        return createValues(ELEMENTS_SIZE,
                            "OU.name.");
    }

    private List<String> createRepositories() {
        return createValues(ELEMENTS_SIZE,
                            "REPO.name.");
    }

    private List<String> createBranches() {
        return createValues(ELEMENTS_SIZE,
                            "Branch.name.");
    }

    private List<Module> createModules() {
        List<Module> elements = new ArrayList<>();
        for (int i = 0; i < ELEMENTS_SIZE; i++) {
            Module module = mock(Module.class);
            when(module.getModuleName()).thenReturn("Module.name." + i);
            elements.add(module);
        }
        return elements;
    }

    private List<String> createValues(int size,
                                      String PREFIX) {
        List<String> elements = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            elements.add(PREFIX + i);
        }
        return elements;
    }

    private void verifyHandlerNotified() {
        verify(changeHandler,
               times(1)).onContentChange();
    }
}
