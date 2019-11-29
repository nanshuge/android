/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.tools.idea.npw.module.recipes.androidModule

import com.android.SdkConstants
import com.android.SdkConstants.FN_ANDROID_MANIFEST_XML
import com.android.tools.idea.npw.module.recipes.addKotlinIfNeeded
import com.android.tools.idea.npw.module.recipes.addTestDependencies
import com.android.tools.idea.npw.module.recipes.addTests
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleColors
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleStrings
import com.android.tools.idea.wizard.template.ModuleTemplateData
import com.android.tools.idea.wizard.template.RecipeExecutor
import com.android.tools.idea.npw.module.recipes.androidModule.res.values.androidModuleStyles
import com.android.tools.idea.npw.module.recipes.copyIcons
import com.android.tools.idea.npw.module.recipes.createDefaultDirectories
import com.android.tools.idea.npw.module.recipes.generateManifest
import com.android.tools.idea.npw.module.recipes.gitignore
import com.android.tools.idea.npw.module.recipes.proguardRecipe
import com.android.tools.idea.wizard.template.FormFactor

fun RecipeExecutor.generateAndroidModule(
  data: ModuleTemplateData,
  appTitle: String?, // may be null only for libraries
  includeCppSupport: Boolean = false,
  cppFlags: String = ""
) {
  val (projectData, srcOut, resOut, manifestOut, testOut, unitTestOut, _, moduleOut) = data
  val language = projectData.language
  val isLibraryProject = data.isLibrary
  val packageName = data.packageName
  val apis = data.apis
  val buildApi = apis.buildApi!!
  val targetApi = apis.targetApi
  val minApi = apis.minApiLevel
  val useAndroidX = projectData.androidXSupport

  createDefaultDirectories(moduleOut, srcOut, resOut)
  addIncludeToSettings(data.name)

  save(
    buildGradle(
      isLibraryProject,
      data.baseFeature != null,
      packageName,
      apis.buildApiString!!,
      projectData.buildToolsVersion,
      minApi,
      targetApi,
      projectData.androidXSupport,
      language,
      projectData.gradlePluginVersion,
      includeCppSupport,
      cppFlags
    ),
    moduleOut.resolve("build.gradle")
  )
  addDependency("com.android.support:appcompat-v7:${buildApi}.+")
  save(generateManifest(packageName, !isLibraryProject), manifestOut.resolve(FN_ANDROID_MANIFEST_XML))
  save(gitignore(), moduleOut.resolve(".gitignore"))
  addTests(packageName, useAndroidX, isLibraryProject, testOut, unitTestOut, language)
  addTestDependencies(projectData.gradlePluginVersion)
  proguardRecipe(moduleOut, data.isLibrary)

  if (!isLibraryProject) {
    copyIcons(resOut)
    with(resOut.resolve(SdkConstants.FD_RES_VALUES)) {
      save(androidModuleStrings(appTitle!!), resolve("strings.xml"))
      save(androidModuleStyles(), resolve("styles.xml"))
      save(androidModuleColors(), resolve("colors.xml"))
    }
  }

  addKotlinIfNeeded(projectData)

  // Unique to mobile module
  if (projectData.hasFormFactor(FormFactor.Mobile) && projectData.hasFormFactor(FormFactor.Wear)) {
    addDependency("com.google.android.gms:play-services-wearable:+", "compile")
  }

  if (includeCppSupport) {
    with(moduleOut.resolve("src/main/cpp")) {
      createDirectory(this)
      save(cMakeListsTxt(), resolve("CMakeLists.txt"))
    }
  }
}
