/*
 * Copyright 2010 the original author or authors.
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
package packager

import org.gradle.testfixtures.ProjectBuilder
import packager.commands.Command
import packager.fakes.TestProject
import spock.lang.Specification

class UbuntuPackagerPluginSpec extends Specification {

    def project = ProjectBuilder.builder().build()
    def plugin = new UbuntuPackagerPlugin(new TestPackager())

    def methodMissing(String name, args) {
        project."$name"(*args)
    }

    def "apply plugin:ubuntu installs the ubuntu packager plugin"() {
        when:
        apply plugin:'ubuntu'

        then:
        assert project.tasks.deb
        assert project.tasks.cleanDeb
        assert project.convention.plugins.ubuntu in UbuntuConvention
    }

    def "the applied deb task should delegate to the packager"() {
        given:
        def project = new TestProject()
        plugin.apply(project)
        project.convention.plugins.ubuntu = new TestConvention(commands)

        when:
        project.getTask('deb').tasks*.execute()

        then:
        plugin.packager.called
        plugin.packager.commands == commands

        where:
        commands << [
                [],
                [{} as Command],
                [{} as Command, {} as Command]
        ]
    }

    def "the packager plugin should use the UbuntuPackager by default"() {
        expect:
        new UbuntuPackagerPlugin().packager in UbuntuPackager
    }

    def "the clean task should delete the convention.workDir"() {
        given:
        UbuntuConvention convention = setupForCleanTask()
        convention.workDir.mkdirs()

        when:
        project.getTask('cleanDeb').tasks*.execute()

        then:
        !convention.workDir.exists()
    }

    def "the clean task should delete artifacts generated by debuild"() {
        given:
        UbuntuConvention convention = setupForCleanTask(projectName, projectVersion)
        def artifact = new File(convention.workDir.parent, "${project.name}_$project.version$suffix")
        artifact.text = suffix

        when:
        project.getTask('cleanDeb').tasks*.execute()

        then:
        !artifact.exists()

        where:
        projectName | projectVersion | suffix
        'test'      | '0.1'          | '.tar.gz'
        'test2'     | '0.1'          | '.tar.gz'
        'test'      | '0.2'          | '.tar.gz'
        'test'      | '0.1'          | '_i386.deb'
        'test'      | '0.1'          | '_amd64.deb'
        'test'      | '0.1'          | '_i386.build'
        'test'      | '0.1'          | '.dsc'
    }

    private def setupForCleanTask(name='test', version='undefined') {
        project = new TestProject(name, version)
        project.buildDir = new File('build/ubuntupackagerplugin')
        plugin.apply(project)
        UbuntuConvention convention = project.convention.plugins.ubuntu
        return convention
    }

    private static final class TestPackager implements Packager {
        def called = false
        def commands

        void execute(List<Command> commands) {
            called = true
            this.commands = commands
        }
    }

    private static final class TestConvention implements PackagerConvention {
        private final List<Command> commands

        TestConvention(List<Command> commands) {
            this.commands = commands
        }

        List<Command> toCommands() {
            commands
        }
    }
}
