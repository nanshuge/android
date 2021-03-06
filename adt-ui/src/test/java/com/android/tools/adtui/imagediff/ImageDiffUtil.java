/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.adtui.imagediff;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.io.File.separatorChar;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.testutils.TestResources;
import com.android.testutils.TestUtils;
import com.android.tools.adtui.TreeWalker;
import com.android.tools.adtui.common.AdtUiUtils;
import com.google.common.io.Files;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;

/**
 * Utility methods to be used by the tests of {@link com.android.tools.adtui.imagediff} package.
 */
public final class ImageDiffUtil {
  /**
   * Default threshold to be used when comparing two images.
   * If the calculated difference between the images is greater than this value (in %), the test should fail.
   */
  public static final double DEFAULT_IMAGE_DIFF_THRESHOLD_PERCENT = 0.5;

  private static final String TEST_DATA_DIR = "/imagediff/";

  private static final String DEFAULT_FONT_PATH = "/fonts/OpenSans-Regular.ttf";

  private static final float DEFAULT_FONT_SIZE = 12f;

  private static final String IMG_DIFF_TEMP_DIR = getTempDir() + "/imagediff";

  /**
   * Unmodifiable list containing all the {@link ImageDiffEntry} of the imagediff package.
   * They are used for running the tests of {@link com.android.tools.adtui.imagediff} package as well as exporting its baseline images.
   * When a generator is implemented, its entries should be included in this list.
   */
  public static final List<ImageDiffEntry> IMAGE_DIFF_ENTRIES = Collections.unmodifiableList(new ArrayList<ImageDiffEntry>() {{
    addAll(new AccordionEntriesRegistrar().getImageDiffEntries());
    addAll(new EventEntriesRegistrar().getImageDiffEntries());
    addAll(new HTreeChartEntriesRegistrar().getImageDiffEntries());
    addAll(new LegendComponentRegistrar().getImageDiffEntries());
    addAll(new LineChartEntriesRegistrar().getImageDiffEntries());
    addAll(new RangeEntriesRegistrar().getImageDiffEntries());
    addAll(new StateChartEntriesRegistrar().getImageDiffEntries());
    addAll(new CommonTabbedPaneEntriesRegistrar().getImageDiffEntries());
  }});

  static {
    // Create tmpDir in case it doesn't exist
    new File(IMG_DIFF_TEMP_DIR).mkdirs();
  }

  private ImageDiffUtil() {
  }

  /**
   * Compares a generated image with a baseline one. If the images differ by more than a determined percentage (similarityThreshold),
   * an image containing the expected, actual and diff images is generated and the test that calls this method fails.
   *
   * @param baselineImageFilename filename of the baseline image
   * @param generatedImage image generated by a test
   * @param similarityThreshold how much (in percent) the baseline and the generated images can differ to still be considered similar
   */
  public static void assertImagesSimilar(String baselineImageFilename, BufferedImage generatedImage, double similarityThreshold) {
    File baselineImageFile = TestResources.getFile(ImageDiffUtil.class, TEST_DATA_DIR + baselineImageFilename);
    BufferedImage baselineImage;

    try {
      baselineImage = convertToARGB(ImageIO.read(baselineImageFile));
      assertImageSimilar(baselineImageFilename, baselineImage, generatedImage, similarityThreshold);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public static void assertImagesSimilar(String baselineImageFilename, Component component, double scaleFactor,
                                         double similarityThreshold) {
    int scaledWidth = (int)Math.round(component.getWidth() * scaleFactor);
    int scaledHeight = (int)Math.round(component.getHeight() * scaleFactor);
    //noinspection UndesirableClassUsage
    BufferedImage buffer = new BufferedImage(scaledWidth, scaledHeight, TYPE_INT_ARGB);
    Graphics2D g = buffer.createGraphics();
    try {
      g.scale(scaleFactor, scaleFactor);
      component.paint(g);
    }
    finally {
      g.dispose();
    }
    File dir = TestResources.getDirectory(ImageDiffUtil.class, TEST_DATA_DIR);
    File goldenFile = new File(dir, baselineImageFilename);
    if (goldenFile.exists()) {
      assertImagesSimilar(baselineImageFilename, buffer, similarityThreshold);
    }
    else {
      //noinspection ResultOfMethodCallIgnored
      goldenFile.getParentFile().mkdirs();
      exportBaselineImage(goldenFile, buffer);
      fail("File did not exist, created here:" + goldenFile);
    }
  }

  /**
   * Exports an image as a baseline.
   *
   * @param destinationFile where the image should be exported to
   * @param image image to be exported
   */
  public static void exportBaselineImage(File destinationFile, BufferedImage image) {
    try {
      ImageIO.write(image, "PNG", destinationFile);
    } catch (IOException e) {
      System.err.println("Caught IOException while trying to export a baseline image: " + destinationFile.getName());
    }
  }

  /**
   * Creates a {@link BufferedImage} from a Swing component.
   */
  public static BufferedImage getImageFromComponent(Component component) {
    // Call doLayout in the content pane and its children
    synchronized (component.getTreeLock()) {
      TreeWalker walker = new TreeWalker(component);
      walker.descendantStream().forEach(Component::doLayout);
    }

    @SuppressWarnings("UndesirableClassUsage") // Don't want Retina images in unit tests
    BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    component.printAll(g);
    g.dispose();

    return image;
  }

  /**
   * Converts a BufferedImage type to {@link TYPE_INT_ARGB},
   * which is the only type accepted by {@link ImageDiffUtil#assertImageSimilar}.
   */
  public static BufferedImage convertToARGB(@NotNull BufferedImage inputImg) {
    if (inputImg.getType() == TYPE_INT_ARGB) {
      return inputImg; // Early return in case the image has already the correct type
    }
    @SuppressWarnings("UndesirableClassUsage") // Don't want Retina images in unit tests
    BufferedImage outputImg = new BufferedImage(inputImg.getWidth(), inputImg.getHeight(), TYPE_INT_ARGB);
    Graphics2D g2d = outputImg.createGraphics();
    g2d.drawImage(inputImg, 0, 0, null);
    g2d.dispose();
    return outputImg;
  }

  /**
   * This font can be used in image comparison tests that generate images containing a lot of text.
   * As logical fonts might differ considerably depending on the OS and JDK, using a TrueType Font is safer.
   */
  public static Font getDefaultFont() {
    try {
      Font font = Font.createFont(Font.TRUETYPE_FONT, TestResources.getFile(ImageDiffUtil.class, DEFAULT_FONT_PATH));
      // Font is created with 1pt, so deriveFont can be used to resize it.
      return font.deriveFont(DEFAULT_FONT_SIZE);

    } catch (IOException | FontFormatException e) {
      System.err.println("Couldn't load default TrueType Font. Using a logical font instead.");
      return AdtUiUtils.DEFAULT_FONT;
    }
  }
  public static void assertImageSimilar(@NotNull File goldenFile,
                                        @NotNull BufferedImage actual,
                                        double maxPercentDifferent) throws IOException {
    if (!goldenFile.exists()) {
      Files.createParentDirs(goldenFile);
      ImageIO.write(actual, "PNG", goldenFile);
      fail("File did not exist, created here:" + goldenFile);
    }

    BufferedImage goldenImage = ImageIO.read(goldenFile);
    assert goldenImage != null : "Failed to read image from " + goldenFile.getAbsolutePath();
    assertImageSimilar(goldenFile.getName(), goldenImage, actual, maxPercentDifferent);
  }

  public static void assertImageSimilar(@NotNull String imageName,
                                        @NotNull BufferedImage goldenImage,
                                        @NotNull BufferedImage image,
                                        double maxPercentDifferent) throws IOException {
    goldenImage = convertToARGB(goldenImage);
    image = convertToARGB(image);

    int imageWidth = Math.min(goldenImage.getWidth(), image.getWidth());
    int imageHeight = Math.min(goldenImage.getHeight(), image.getHeight());

    // Blur the images to account for the scenarios where there are pixel
    // differences
    // in where a sharp edge occurs
    // goldenImage = blur(goldenImage, 6);
    // image = blur(image, 6);

    int width = 3 * imageWidth;
    @SuppressWarnings("UnnecessaryLocalVariable")
    int height = imageHeight; // makes code more readable
    @SuppressWarnings("UndesirableClassUsage") // Don't want Retina images in unit tests
    BufferedImage deltaImage = new BufferedImage(width, height, TYPE_INT_ARGB);
    Graphics g = deltaImage.getGraphics();

    // Compute delta map
    double delta = 0;
    for (int y = 0; y < imageHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        int goldenRgb = goldenImage.getRGB(x, y);
        int rgb = image.getRGB(x, y);
        if (goldenRgb == rgb) {
          deltaImage.setRGB(imageWidth + x, y, 0x00808080);
          continue;
        }

        // If the pixels have no opacity, don't delta colors at all
        if (((goldenRgb & 0xFF000000) == 0) && (rgb & 0xFF000000) == 0) {
          deltaImage.setRGB(imageWidth + x, y, 0x00808080);
          continue;
        }

        int deltaA = ((rgb & 0xFF000000) >>> 24) - ((goldenRgb & 0xFF000000) >>> 24);
        int newA = 128 + deltaA & 0xFF;
        int deltaR = ((rgb & 0xFF0000) >>> 16) - ((goldenRgb & 0xFF0000) >>> 16);
        int newR = 128 + deltaR & 0xFF;
        int deltaG = ((rgb & 0x00FF00) >>> 8) - ((goldenRgb & 0x00FF00) >>> 8);
        int newG = 128 + deltaG & 0xFF;
        int deltaB = (rgb & 0x0000FF) - (goldenRgb & 0x0000FF);
        int newB = 128 + deltaB & 0xFF;

        int newRGB = newA << 24 | newR << 16 | newG << 8 | newB;
        deltaImage.setRGB(imageWidth + x, y, newRGB);

        double dA = deltaA / 255.;
        double dR = deltaR / 255.;
        double dG = deltaG / 255.;
        double dB = deltaB / 255.;
        // Notice that maximum difference per pixel is 1, which is realized for completely opaque black and white colors.
        delta += Math.sqrt((dA * dA + dR * dR + dG * dG + dB * dB) / 4.);
      }
    }

    double maxDiff = imageHeight * imageWidth;
    double percentDifference = (delta / maxDiff) * 100;

    String error = null;
    if (percentDifference > maxPercentDifferent) {
      error = String.format("Images differ (by %.2g%%)", percentDifference);
    } else if (Math.abs(goldenImage.getWidth() - image.getWidth()) >= 2) {
      error = "Widths differ too much for " + imageName + ": " + goldenImage.getWidth() + "x" + goldenImage.getHeight() +
              "vs" + image.getWidth() + "x" + image.getHeight();
    } else if (Math.abs(goldenImage.getHeight() - image.getHeight()) >= 2) {
      error = "Heights differ too much for " + imageName + ": " + goldenImage.getWidth() + "x" + goldenImage.getHeight() +
              "vs" + image.getWidth() + "x" + image.getHeight();
    }

    if (error != null) {
      // Expected on the left
      // Golden on the right
      g.drawImage(goldenImage, 0, 0, null);
      g.drawImage(image, 2 * imageWidth, 0, null);

      // Labels
      if (imageWidth > 80) {
        g.setColor(Color.RED);
        g.drawString("Expected", 10, 20);
        g.drawString("Actual", 2 * imageWidth + 10, 20);
      }

      // Write image diff to undeclared outputs dir so ResultStore archives.
      File output = new File(TestUtils.getTestOutputDir(), "delta-" + imageName.replace(separatorChar, '_'));
      if (output.exists()) {
        boolean deleted = output.delete();
        assertTrue(deleted);
      }
      ImageIO.write(deltaImage, "PNG", output);
      error += " - see details in archived file " + output.getPath();
      System.out.println(error);
      fail(error);
    }

    g.dispose();
  }

  @NotNull
  // TODO move this function to a common location for all our tests
  public static File getTempDir() {
    if (System.getProperty("os.name").equals("Mac OS X")) {
      return new File("/tmp"); //$NON-NLS-1$
    }

    return new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
  }
}
