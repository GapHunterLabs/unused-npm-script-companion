package dev.gaphunter.unusednpmscriptcompanion.scan

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CiFileLocatorTest : BasePlatformTestCase() {

    private fun packageJsonDir() = myFixture.addFileToProject(
        "package.json",
        """{ "scripts": { "test": "jest" } }""",
    ).virtualFile.parent

    fun `test finds a GitHub Actions workflow file`() {
        myFixture.addFileToProject(".github/workflows/ci.yml", "run: npm test")
        val found = CiFileLocator.findCiFiles(packageJsonDir())
        assertTrue(found.any { it.name == "ci.yml" })
    }

    fun `test finds a GitLab CI file`() {
        myFixture.addFileToProject(".gitlab-ci.yml", "script: npm test")
        val found = CiFileLocator.findCiFiles(packageJsonDir())
        assertTrue(found.any { it.name == ".gitlab-ci.yml" })
    }

    fun `test finds a CircleCI config file`() {
        myFixture.addFileToProject(".circleci/config.yml", "run: npm test")
        val found = CiFileLocator.findCiFiles(packageJsonDir())
        assertTrue(found.any { it.name == "config.yml" })
    }

    fun `test a project with no CI files at all returns an empty list`() {
        val found = CiFileLocator.findCiFiles(packageJsonDir())
        assertTrue(found.isEmpty())
    }

    fun `test all three CI systems present at once are all found`() {
        myFixture.addFileToProject(".github/workflows/ci.yml", "run: npm test")
        myFixture.addFileToProject(".gitlab-ci.yml", "script: npm test")
        myFixture.addFileToProject(".circleci/config.yml", "run: npm test")
        val found = CiFileLocator.findCiFiles(packageJsonDir())
        assertEquals(3, found.size)
    }
}
