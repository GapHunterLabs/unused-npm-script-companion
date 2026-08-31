package dev.gaphunter.unusednpmscriptcompanion.scan

import com.intellij.openapi.vfs.VirtualFile

/**
 * Finds the CI config files this plugin scans for script references,
 * relative to the directory a `package.json` lives in. **v0.1/v0.2
 * scope, stated honestly (see README "CI formats covered"): GitHub
 * Actions (every YAML file directly under `.github/workflows`),
 * GitLab CI (`.gitlab-ci.yml`), and CircleCI (`.circleci/config.yml`)
 * only** -- the most common CI systems, per the task brief.
 * `Jenkinsfile` and `azure-pipelines.yml` are explicitly deferred to a
 * future version, not silently unsupported (same "documented gap, not
 * a hidden one" discipline as `GradleBuildFileParser`'s configuration
 * list).
 */
object CiFileLocator {

    /** Returns every CI config file found next to [packageJsonDir], v0.1/v0.2 formats only. */
    fun findCiFiles(packageJsonDir: VirtualFile): List<VirtualFile> {
        val results = mutableListOf<VirtualFile>()

        val workflowsDir = packageJsonDir.findChild(".github")?.findChild("workflows")
        if (workflowsDir != null && workflowsDir.isDirectory) {
            for (child in workflowsDir.children) {
                if (!child.isDirectory && (child.extension == "yml" || child.extension == "yaml")) {
                    results.add(child)
                }
            }
        }

        val gitlabCi = packageJsonDir.findChild(".gitlab-ci.yml")
        if (gitlabCi != null && !gitlabCi.isDirectory) {
            results.add(gitlabCi)
        }

        // CircleCI: a single fixed path, unlike GitHub Actions' whole
        // directory of arbitrarily-named workflow files -- no wildcard
        // scan needed.
        val circleCi = packageJsonDir.findChild(".circleci")?.findChild("config.yml")
        if (circleCi != null && !circleCi.isDirectory) {
            results.add(circleCi)
        }

        return results
    }

    /** Returns `README.md` next to [packageJsonDir], if present. */
    fun findReadme(packageJsonDir: VirtualFile): VirtualFile? {
        val readme = packageJsonDir.findChild("README.md")
        return if (readme != null && !readme.isDirectory) readme else null
    }
}
