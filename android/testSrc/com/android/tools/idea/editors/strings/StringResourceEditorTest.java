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
package com.android.tools.idea.editors.strings;

import static org.junit.Assert.assertEquals;

import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import java.awt.Font;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class StringResourceEditorTest {
  private Font myFont;
  private float myOldScale;

  @Before
  public void initFont() {
    myFont = new Font(Font.DIALOG, Font.PLAIN, 12);
  }

  @Before
  public void setScale() {
    myOldScale = JBUI.scale(1F);
    JBUI.setUserScaleFactor(2F);
  }

  @After
  public void resetScaleToOldValue() {
    JBUI.setUserScaleFactor(myOldScale);
  }

  @Test
  public void getFontScalesFonts() {
    // Act
    Font font = StringResourceEditor.getFont("", myFont, true);

    // Assert
    assertEquals(24, font.getSize());
  }

  @Test
  public void getFontDoesntScaleJBFonts() {
    // Arrange
    Font font = JBFont.create(myFont);

    // Act
    font = StringResourceEditor.getFont("", font, true);

    // Assert
    assertEquals(24, font.getSize());
  }
}
