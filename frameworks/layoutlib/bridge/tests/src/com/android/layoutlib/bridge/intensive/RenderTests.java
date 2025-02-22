/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.layoutlib.bridge.intensive;

import com.android.ide.common.rendering.api.RenderSession;
import com.android.ide.common.rendering.api.ResourceNamespace;
import com.android.ide.common.rendering.api.ResourceReference;
import com.android.ide.common.rendering.api.ResourceValueImpl;
import com.android.ide.common.rendering.api.Result;
import com.android.ide.common.rendering.api.SessionParams;
import com.android.ide.common.rendering.api.SessionParams.RenderingMode;
import com.android.ide.common.rendering.api.ViewInfo;
import com.android.ide.common.rendering.api.XmlParserFactory;
import com.android.internal.R;
import com.android.internal.lang.System_Delegate;
import com.android.layoutlib.bridge.android.BridgeContext;
import com.android.layoutlib.bridge.android.RenderParamsFlags;
import com.android.layoutlib.bridge.android.RenderTestBase;
import com.android.layoutlib.bridge.impl.ParserFactory;
import com.android.layoutlib.bridge.impl.RenderAction;
import com.android.layoutlib.bridge.impl.RenderActionTestUtil;
import com.android.layoutlib.bridge.impl.ResourceHelper;
import com.android.layoutlib.bridge.intensive.setup.ConfigGenerator;
import com.android.layoutlib.bridge.intensive.setup.LayoutPullParser;
import com.android.resources.Density;
import com.android.resources.Navigation;
import com.android.resources.ResourceType;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import android.R.attr;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Resources_Delegate;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.LinearLayout;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Set of render tests
 */
public class RenderTests extends RenderTestBase {

    @After
    public void afterTestCase() {
        com.android.layoutlib.bridge.test.widgets.HookWidget.reset();
    }

    @Test
    public void testActivity() throws ClassNotFoundException, FileNotFoundException {
        renderAndVerify("activity.xml", "activity.png", true);
    }

    @Test
    public void testActivityOnOldTheme() throws ClassNotFoundException, FileNotFoundException {
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<RelativeLayout xmlns:android=\"http://schemas" +
                        ".android.com/apk/res/android\"\n" +
                        "                android:layout_width=\"match_parent\"\n" +
                        "                android:layout_height=\"match_parent\"\n" +
                        "                android:paddingLeft=\"@dimen/activity_horizontal_margin"
                        + "\"\n"
                        +
                        "                android:paddingRight=\"@dimen/activity_horizontal_margin"
                        + "\"\n"
                        +
                        "                android:paddingTop=\"@dimen/activity_vertical_margin\"\n" +
                        "                android:paddingBottom=\"@dimen/activity_vertical_margin"
                        + "\">\n"
                        +
                        "    <TextView\n" +
                        "        android:text=\"@string/hello_world\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"200dp\"\n" +
                        "        android:background=\"#FF0000\"\n" +
                        "        android:id=\"@+id/text1\"/>\n" +
                        "</RelativeLayout>");
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.NoTitleBar", false)
                .build();

        renderAndVerify(params, "simple_activity-old-theme.png");
    }

    @Test
    public void testTranslucentBars() throws ClassNotFoundException, FileNotFoundException {
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        LayoutPullParser parser = createParserFromPath("four_corners.xml");
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.TranslucentDecor", false)
                .build();
        renderAndVerify(params, "four_corners_translucent.png");

        parser = createParserFromPath("four_corners.xml");
        params = getSessionParamsBuilder()
                .setConfigGenerator(ConfigGenerator.NEXUS_5_LAND)
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.TranslucentDecor", false)
                .build();
        renderAndVerify(params, "four_corners_translucent_land.png");

        parser = createParserFromPath("four_corners.xml");
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar", false)
                .build();
        renderAndVerify(params, "four_corners.png");
    }

    @Test
    public void testAllWidgets() throws ClassNotFoundException, FileNotFoundException {
        LayoutPullParser parser = createParserFromPath("allwidgets.xml");
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(ConfigGenerator.NEXUS_5)
                .setCallback(layoutLibCallback)
                .build();

        renderAndVerify(params, "allwidgets.png");
    }

    @Test
    public void testArrayCheck() throws ClassNotFoundException, FileNotFoundException {
        renderAndVerify("array_check.xml", "array_check.png", false);
    }

    @Test
    public void testAllWidgetsTablet() throws ClassNotFoundException, FileNotFoundException {
        LayoutPullParser parser = createParserFromPath("allwidgets.xml");
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(ConfigGenerator.NEXUS_7_2012)
                .setCallback(layoutLibCallback)
                .build();
        renderAndVerify(params, "allwidgets_tab.png");
    }

    @Test
    public void testActivityActionBar() throws ClassNotFoundException {
        String simpleActivity =
                "<RelativeLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "                android:layout_width=\"match_parent\"\n" +
                        "                android:layout_height=\"match_parent\"\n" +
                        "                android:paddingLeft=\"@dimen/activity_horizontal_margin"
                        + "\"\n"
                        +
                        "                android:paddingRight=\"@dimen/activity_horizontal_margin"
                        + "\"\n"
                        +
                        "                android:paddingTop=\"@dimen/activity_vertical_margin\"\n" +
                        "                android:paddingBottom=\"@dimen/activity_vertical_margin"
                        + "\">\n"
                        +
                        "    <TextView\n" +
                        "        android:text=\"@string/hello_world\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"200dp\"\n" +
                        "        android:background=\"#FF0000\"\n" +
                        "        android:id=\"@+id/text1\"/>\n" +
                        "</RelativeLayout>";

        LayoutPullParser parser = LayoutPullParser.createFromString(simpleActivity);
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "simple_activity_noactionbar.png");

        parser = LayoutPullParser.createFromString(simpleActivity);
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "simple_activity.png");

        // This also tests that a theme with "NoActionBar" DOES HAVE an action bar when we are
        // displaying menus.
        parser = LayoutPullParser.createFromString(simpleActivity);
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ROOT_TAG, "menu");
        renderAndVerify(params, "simple_activity.png");
    }

    @Test
    public void testOnApplyInsetsCall()
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        // We get the widget via reflection to avoid IntelliJ complaining about the class being
        // located in the wrong package. (From the Bridge tests point of view, it is)
        Class insetsWidgetClass = Class.forName("com.android.layoutlib.test.myapplication.widgets" +
                ".InsetsWidget");
        Field field = insetsWidgetClass.getDeclaredField("sApplyInsetsCalled");
        assertFalse((Boolean) field.get(null));

        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"wrap_content\"\n" +
                        "              android:layout_height=\"wrap_content\">\n" + "\n" +
                        "    <com.android.layoutlib.test.myapplication.widgets.InsetsWidget\n" +
                        "        android:text=\"Hello world\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:id=\"@+id/text1\"/>\n" + "</LinearLayout>\n");
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar", false)
                .build();

        render(sBridge, params, -1);

        assertTrue((Boolean) field.get(null));
        field.set(null, false);
    }

    /** Test expand_layout.xml */
    @Test
    public void testExpand() throws ClassNotFoundException, FileNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = createParserFromPath("expand_vert_layout.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        ConfigGenerator customConfigGenerator = new ConfigGenerator()
                .setScreenWidth(300)
                .setScreenHeight(20)
                .setDensity(Density.XHIGH)
                .setNavigation(Navigation.NONAV);

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(customConfigGenerator)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "expand_vert_layout.png");

        customConfigGenerator = new ConfigGenerator()
                .setScreenWidth(20)
                .setScreenHeight(300)
                .setDensity(Density.XHIGH)
                .setNavigation(Navigation.NONAV);
        parser = createParserFromPath("expand_horz_layout.xml");
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(customConfigGenerator)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.H_SCROLL)
                .build();

        renderAndVerify(params, "expand_horz_layout.png");
    }

    @Test
    public void testShrink() throws ClassNotFoundException, FileNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = createParserFromPath("expand_vert_layout.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        // Normal mode
        ConfigGenerator customConfigGenerator = new ConfigGenerator()
                .setScreenWidth(600)
                .setScreenHeight(3000)
                .setDensity(Density.XHIGH)
                .setNavigation(Navigation.NONAV);
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(customConfigGenerator)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.NORMAL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "normal_layout.png");

        // Shrink mode
        customConfigGenerator = new ConfigGenerator()
                .setScreenWidth(600)
                .setScreenHeight(3000)
                .setDensity(Density.XHIGH)
                .setNavigation(Navigation.NONAV);
        parser = createParserFromPath("expand_vert_layout.xml");
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(customConfigGenerator)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.SHRINK)
                .disableDecoration()
                .build();

        renderAndVerify(params, "shrunk_layout.png");
    }

    /** Test indeterminate_progressbar.xml */
    @Test
    public void testVectorAnimation() throws ClassNotFoundException {
        String layout = "\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "              android:padding=\"16dp\"\n" +
                "              android:orientation=\"horizontal\"\n" +
                "              android:layout_width=\"fill_parent\"\n" +
                "              android:layout_height=\"fill_parent\">\n" + "\n" +
                "    <ProgressBar\n" + "             android:layout_height=\"fill_parent\"\n" +
                "             android:layout_width=\"fill_parent\" />\n" + "\n" +
                "</LinearLayout>\n";

        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "animated_vector.png", TimeUnit.SECONDS.toNanos(2));

        parser = LayoutPullParser.createFromString(layout);
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        renderAndVerify(params, "animated_vector_1.png", TimeUnit.SECONDS.toNanos(3));
    }

    /**
     * Test a vector drawable that uses trimStart and trimEnd. It also tests all the primitives
     * for vector drawables (lines, moves and cubic and quadratic curves).
     */
    @Test
    public void testVectorDrawable() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"fill_parent\"\n" +
                        "             android:layout_width=\"fill_parent\"\n" +
                        "             android:src=\"@drawable/multi_path\" />\n" + "\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .disableDecoration()
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable.png", TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Regression test for http://b.android.com/91383 and http://b.android.com/203797
     */
    @Test
    public void testVectorDrawable91383() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"vertical\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <ImageView\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:src=\"@drawable/android\"/>\n" +
                        "    <ImageView\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:src=\"@drawable/headset\"/>\n" +
                        "    <ImageView\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:src=\"@drawable/clipped_even_odd\"/>\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_91383.png", TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Test a ImageView which has a vector drawable as its src and tint attribute.
     */
    @Test
    public void testVectorDrawableWithTintInImageView() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<ImageView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:src=\"@drawable/vector_drawable_without_tint\"\n" +
                        "    android:tint=\"#FF00FF00\" />");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_with_tint_in_image_view.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Test a vector drawable which has tint attribute.
     */
    @Test
    public void testVectorDrawableWithTintInItself() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<ImageView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:src=\"@drawable/vector_drawable_with_tint\" />");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_with_tint_itself.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Test a vector drawable which is transparent.
     */
    @Test
    public void testTransparentDrawable() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<ImageView xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_height=\"fill_parent\"\n" +
                        "    android:layout_width=\"fill_parent\"\n" +
                        "    android:src=\"@drawable/transparent_drawable\" />");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .setTransparentBackground()
                .build();

        renderAndVerify(params, "transparent_drawable.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Test a vector drawable that uses trimStart and trimEnd. It also tests all the primitives
     * for vector drawables (lines, moves and cubic and quadratic curves).
     */
    @Ignore("This test does not make sense in layoutlib anymore, test in Studio")
    @Test
    public void testVectorDrawableHasMultipleLineInPathData() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/multi_line_of_path_data\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_multi_line_of_path_data.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Tests that the gradients are correctly transformed using the viewport of the vector drawable.
     * <p/>
     * If a vector drawable is 50x50 and the gradient has startX=25 and startY=25, the gradient
     * will start in the middle of the box.
     * <p/>
     * http://b/65495452
     */
    @Test
    public void testVectorDrawableGradient() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/shadow\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_gradient.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Tests that the radial gradients are correctly transformed using the viewport of the vector
     * drawable.
     * <p/>
     * http://b/66168608
     */
    @Test
    public void testVectorDrawableRadialGradient() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/radial_gradient\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "vector_drawable_radial_gradient.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Tests that the gradients are correctly displayed when using transparent colors
     * and a wide range of offset values.
     * <p/>
     * http://b/112759140
     */
    @Test
    public void testGradientColors() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/gradient\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "gradient_colors.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Tests that the gradients are correctly combined with alpha values.
     * <p/>
     * http://b/122260583
     */
    @Test
    public void testGradientAlphaDrawable() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/vector_gradient_alpha\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "gradient_alpha_drawable.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    /** Test activity.xml */
    @Test
    public void testScrollingAndMeasure() throws ClassNotFoundException, FileNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = createParserFromPath("scrolled.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();
        params.setExtendedViewInfoMode(true);

        // Do an only-measure pass
        RenderSession session = sBridge.createSession(params);
        session.measure();
        RenderResult result = RenderResult.getFromSession(session);
        assertNotNull(result);
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isSuccess());

        ViewInfo rootLayout = result.getRootViews().get(0);
        // Check the first box in the main LinearLayout
        assertEquals(-90, rootLayout.getChildren().get(0).getTop());
        assertEquals(-30, rootLayout.getChildren().get(0).getLeft());
        assertEquals(90, rootLayout.getChildren().get(0).getBottom());
        assertEquals(150, rootLayout.getChildren().get(0).getRight());

        // Check the first box within the nested LinearLayout
        assertEquals(-450, rootLayout.getChildren().get(5).getChildren().get(0).getTop());
        assertEquals(90, rootLayout.getChildren().get(5).getChildren().get(0).getLeft());
        assertEquals(-270, rootLayout.getChildren().get(5).getChildren().get(0).getBottom());
        assertEquals(690, rootLayout.getChildren().get(5).getChildren().get(0).getRight());
        session.dispose();

        // Do a full render pass
        parser = createParserFromPath("scrolled.xml");

        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();
        params.setExtendedViewInfoMode(true);

        result = renderAndVerify(params, "scrolled.png");
        assertNotNull(result);
        assertNotNull(result.getResult());
        assertTrue(result.getResult().isSuccess());
    }

    @Test
    public void testGetResourceNameVariants() throws Exception {
        // Setup
        // Create the layout pull parser for our resources (empty.xml can not be part of the test
        // app as it won't compile).
        LayoutPullParser parser = LayoutPullParser.createFromPath("/empty.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setConfigGenerator(ConfigGenerator.NEXUS_4)
                .setCallback(layoutLibCallback)
                .build();
        AssetManager assetManager = AssetManager.getSystem();
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration configuration = RenderAction.getConfiguration(params);
        BridgeContext context = new BridgeContext(params.getProjectKey(), metrics,
                params.getResources(),
                params.getAssets(), params.getLayoutlibCallback(), configuration,
                params.getTargetSdkVersion(), params.isRtlSupported());
        Resources resources = Resources_Delegate.initSystem(context, assetManager, metrics,
                configuration, params.getLayoutlibCallback());
        // Test
        assertEquals("android:style/ButtonBar",
                resources.getResourceName(android.R.style.ButtonBar));
        assertEquals("android", resources.getResourcePackageName(android.R.style.ButtonBar));
        assertEquals("ButtonBar", resources.getResourceEntryName(android.R.style.ButtonBar));
        assertEquals("style", resources.getResourceTypeName(android.R.style.ButtonBar));
        Integer id = Resources_Delegate.getLayoutlibCallback(resources).getOrGenerateResourceId(
                new ResourceReference(ResourceNamespace.RES_AUTO, ResourceType.STRING, "app_name"));
        assertNotNull(id);
        assertEquals("com.android.layoutlib.test.myapplication:string/app_name",
                resources.getResourceName(id));
        assertEquals("com.android.layoutlib.test.myapplication",
                resources.getResourcePackageName(id));
        assertEquals("string", resources.getResourceTypeName(id));
        assertEquals("app_name", resources.getResourceEntryName(id));

        context.disposeResources();
    }

    @Test
    public void testStringEscaping() throws Exception {
        // Setup
        // Create the layout pull parser for our resources (empty.xml can not be part of the test
        // app as it won't compile).
        LayoutPullParser parser = LayoutPullParser.createFromPath("/empty.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(RenderTestBase.getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setConfigGenerator(ConfigGenerator.NEXUS_4)
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .build();
        AssetManager assetManager = AssetManager.getSystem();
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration configuration = RenderAction.getConfiguration(params);
        BridgeContext context = new BridgeContext(params.getProjectKey(), metrics,
                params.getResources(),
                params.getAssets(), params.getLayoutlibCallback(), configuration,
                params.getTargetSdkVersion(), params.isRtlSupported());
        Resources resources = Resources_Delegate.initSystem(context, assetManager, metrics,
                configuration, params.getLayoutlibCallback());

        Integer id =
                Resources_Delegate.getLayoutlibCallback(resources)
                        .getOrGenerateResourceId(
                                new ResourceReference(
                                        ResourceNamespace.RES_AUTO,
                                        ResourceType.ARRAY,
                                        "string_array"));
        assertNotNull(id);
        String[] strings = resources.getStringArray(id);
        assertArrayEquals(
                new String[]{"mystring", "Hello world!", "candidates", "Unknown", "?EC"},
                strings);
        assertTrue(sRenderMessages.isEmpty());

        context.disposeResources();
    }

    @Test
    public void testFonts() throws ClassNotFoundException, FileNotFoundException {
        // TODO: styles seem to be broken in TextView
        renderAndVerify("fonts_test.xml", "font_test.png", false);
    }

    @Test
    public void testAdaptiveIcon() throws ClassNotFoundException, FileNotFoundException {
        // Create the layout pull parser.
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\"\n" +
                        "             android:src=\"@drawable/adaptive\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50,0L100,0 100,100 0,100 0,0z");
        renderAndVerify(params, "adaptive_icon.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 " +
                        "22.4 22.4 0 50 0Z");
        renderAndVerify(params, "adaptive_icon_circle.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50,0L92,0C96.42,0 100,4.58 100 8L100,92C100, 96.42 96.42 100 92 100L8 100C4.58," +
                        " 100 0 96.42 0 92L0 8 C 0 4.42 4.42 0 8 0L50 0Z");
        renderAndVerify(params, "adaptive_icon_rounded_corners.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50,0 C10,0 0,10 0,50 0,90 10,100 50,100 90,100 100,90 100,50 100,10 90,0 50,0 Z");
        renderAndVerify(params, "adaptive_icon_squircle.png");
    }

    @Test
    public void testTypedValue() throws Exception {
        // Setup
        // Create the layout pull parser for our resources (empty.xml can not be part of the test
        // app as it won't compile).
        LayoutPullParser parser = LayoutPullParser.createFromPath("/empty.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(RenderTestBase.getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setConfigGenerator(ConfigGenerator.NEXUS_4)
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .build();
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration configuration = RenderAction.getConfiguration(params);

        BridgeContext mContext =
                new BridgeContext(params.getProjectKey(), metrics, params.getResources(),
                        params.getAssets(), params.getLayoutlibCallback(), configuration,
                        params.getTargetSdkVersion(), params.isRtlSupported());

        TypedValue outValue = new TypedValue();
        mContext.resolveThemeAttribute(android.R.attr.colorPrimary, outValue, true);
        assertEquals(TypedValue.TYPE_INT_COLOR_ARGB8, outValue.type);
        assertNotEquals(0, outValue.data);

        outValue = new TypedValue();
        mContext.resolveThemeAttribute(android.R.attr.colorError, outValue, true);
        assertEquals(TypedValue.TYPE_INT_COLOR_RGB4, outValue.type);
        assertEquals(-65536, outValue.data);

        outValue = new TypedValue();
        mContext.resolveThemeAttribute(attr.colorActivatedHighlight, outValue, true);
        assertEquals(TypedValue.TYPE_INT_COLOR_ARGB4, outValue.type);
        assertEquals(-872349952, outValue.data);

        outValue = new TypedValue();
        mContext.resolveThemeAttribute(android.R.attr.isLightTheme, outValue, true);
        assertEquals(TypedValue.TYPE_INT_BOOLEAN, outValue.type);
        assertEquals(1, outValue.data);

        outValue = new TypedValue();
        mContext.resolveThemeAttribute(android.R.attr.scrollbarFadeDuration, outValue, true);
        assertEquals(TypedValue.TYPE_INT_DEC, outValue.type);
        assertEquals(250, outValue.data);

        outValue = new TypedValue();
        mContext.resolveThemeAttribute(android.R.attr.scrollbarThumbHorizontal, outValue, true);
        assertEquals(TypedValue.TYPE_STRING, outValue.type);
        assertNotNull(outValue.string);
        assertTrue(sRenderMessages.isEmpty());
    }

    @Test
    public void testColorStateList() throws Exception {
        final String STATE_LIST =
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                        "    <item android:state_pressed=\"true\"\n" +
                        "          android:color=\"?android:attr/colorForeground\"/> \n" +
                        "    <item android:state_focused=\"true\"\n" +
                        "          android:color=\"?android:attr/colorBackground\"/> \n" +
                        "    <item android:color=\"#a000\"/> <!-- default -->\n" + "</selector>";

        File tmpColorList = File.createTempFile("statelist", "xml");
        try (PrintWriter output = new PrintWriter(new FileOutputStream(tmpColorList))) {
            output.println(STATE_LIST);
        }

        // Setup
        // Create the layout pull parser for our resources (empty.xml can not be part of the test
        // app as it won't compile).
        ParserFactory.setParserFactory(new XmlParserFactory() {
            @Override
            @Nullable
            public XmlPullParser createXmlParserForPsiFile(@NonNull String fileName) {
                return null;
            }

            @Override
            @Nullable
            public XmlPullParser createXmlParserForFile(@NonNull String fileName) {
                return null;
            }

            @Override
            @NonNull
            public XmlPullParser createXmlParser() {
                return new KXmlParser();
            }
        });

        LayoutPullParser parser = LayoutPullParser.createFromPath("/empty.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(RenderTestBase.getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setConfigGenerator(ConfigGenerator.NEXUS_4)
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material", false)
                .build();
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration configuration = RenderAction.getConfiguration(params);

        BridgeContext mContext =
                new BridgeContext(params.getProjectKey(), metrics, params.getResources(),
                        params.getAssets(), params.getLayoutlibCallback(), configuration,
                        params.getTargetSdkVersion(), params.isRtlSupported());
        mContext.initResources(params.getAssets());
        BridgeContext oldContext = RenderActionTestUtil.setBridgeContext(mContext);

        try {
            ColorStateList stateList = ResourceHelper.getColorStateList(
                    new ResourceValueImpl(
                            ResourceNamespace.RES_AUTO,
                            ResourceType.COLOR,
                            "test_list",
                            tmpColorList.getAbsolutePath()),
                    mContext,
                    null);
            assertNotNull(stateList);
            assertEquals(Color.parseColor("#ffffffff"), stateList.getColorForState(
                    StateSet.get(StateSet.VIEW_STATE_PRESSED),
                    0
            ));
            assertEquals(Color.parseColor("#ff303030"), stateList.getColorForState(
                    StateSet.get(StateSet.VIEW_STATE_FOCUSED),
                    0
            ));
            assertEquals(Color.parseColor("#AA000000"), stateList.getDefaultColor());

            // Now apply theme overlay and check the colors changed
            Resources.Theme theme = mContext.getResources().newTheme();
            theme.applyStyle(R.style.ThemeOverlay_Material_Light, true);
            stateList = ResourceHelper.getColorStateList(
                    new ResourceValueImpl(
                            ResourceNamespace.RES_AUTO,
                            ResourceType.COLOR,
                            "test_list",
                            tmpColorList.getAbsolutePath()),
                    mContext,
                    theme);
            assertNotNull(stateList);
            assertEquals(Color.parseColor("#ff000000"), stateList.getColorForState(
                    StateSet.get(StateSet.VIEW_STATE_PRESSED),
                    0
            ));
            assertEquals(Color.parseColor("#fffafafa"), stateList.getColorForState(
                    StateSet.get(StateSet.VIEW_STATE_FOCUSED),
                    0
            ));
            assertEquals(Color.parseColor("#AA000000"), stateList.getDefaultColor());
        } finally {
            RenderActionTestUtil.setBridgeContext(oldContext);
        }
        mContext.disposeResources();
    }

    @Test
    public void testResourcesGetIdentifier() throws Exception {
        // Setup
        // Create the layout pull parser for our resources (empty.xml can not be part of the test
        // app as it won't compile).
        LayoutPullParser parser = LayoutPullParser.createFromPath("/empty.xml");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setConfigGenerator(ConfigGenerator.NEXUS_4)
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .build();
        AssetManager assetManager = AssetManager.getSystem();
        DisplayMetrics metrics = new DisplayMetrics();
        Configuration configuration = RenderAction.getConfiguration(params);
        BridgeContext context = new BridgeContext(params.getProjectKey(), metrics,
                params.getResources(),
                params.getAssets(), params.getLayoutlibCallback(), configuration,
                params.getTargetSdkVersion(), params.isRtlSupported());
        Resources resources = Resources_Delegate.initSystem(context, assetManager, metrics,
                configuration, params.getLayoutlibCallback());
        Integer id =
                Resources_Delegate.getLayoutlibCallback(resources)
                        .getOrGenerateResourceId(
                                new ResourceReference(
                                        ResourceNamespace.RES_AUTO,
                                        ResourceType.STRING,
                                        "app_name"));
        assertNotNull(id);
        assertEquals(id.intValue(),
                resources.getIdentifier("com.android.layoutlib.test.myapplication:string/app_name",
                        null, null));
        assertEquals(id.intValue(), resources.getIdentifier("app_name", "string",
                "com.android.layoutlib.test.myapplication"));
        assertEquals(0, resources.getIdentifier("string/app_name", null, null));
        assertEquals(0, resources.getIdentifier("string/app_name", null, "com.foo.bar"));
        assertEquals(0, resources.getIdentifier("string/does_not_exist", null,
                "com.android.layoutlib.test.myapplication"));
        assertEquals(R.string.accept, resources.getIdentifier("android:string/accept", null,
                null));
        assertEquals(R.string.accept, resources.getIdentifier("string/accept", null,
                "android"));
        assertEquals(R.id.message, resources.getIdentifier("id/message", null,
                "android"));
        assertEquals(R.string.accept, resources.getIdentifier("accept", "string",
                "android"));

        context.disposeResources();
    }

    /**
     * If a 9patch image was in the nodpi or anydpi folder, the density of the image was 0 resulting
     * in a float division by 0 and thus an infinite padding
     * when layoutlib tries to scale the padding of the 9patch.
     *
     * http://b/37136109
     */
    @Test
    public void test9PatchNoDPIBackground() throws Exception {
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        String layoutCompiled =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:background=\"@drawable/ninepatch\"\n" +
                        "    android:layout_margin=\"20dp\"\n" +
                        "    android:orientation=\"vertical\">\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n"
                        + "</LinearLayout>";

        LayoutPullParser parser = LayoutPullParser.createFromString(layoutCompiled);
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "ninepatch_background.png");

        String layoutNonCompiled =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:background=\"@drawable/uncompiled_ninepatch\"\n" +
                        "    android:layout_margin=\"20dp\"\n" +
                        "    android:orientation=\"vertical\">\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n"
                        + "</LinearLayout>";

        parser = LayoutPullParser.createFromString(layoutNonCompiled);
        params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "ninepatch_background.png");
    }

    @Test
    public void testAssetManager() throws Exception {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <com.android.layoutlib.test.myapplication.widgets.AssetView\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "asset.png");
    }

    /**
     * Tests that calling setTheme in a ContextThemeWrapper actually applies the theme
     *
     * http://b/66902070
     */
    @Test
    public void testContextThemeWrapper() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<com.android.layoutlib.test.myapplication.ThemableWidget " +
                        "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"wrap_content\"\n" +
                        "              android:layout_height=\"wrap_content\" />\n");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();

        renderAndVerify(params, "context_theme_wrapper.png", TimeUnit.SECONDS.toNanos(2));
    }

    /**
     * Tests that a crashing view does not prevent others from working. This is meant to prevent
     * crashes in framework views since custom views are already handled by Android Studio by
     * rewriting the byte code.
     */
    @Test
    public void testCrashes() throws ClassNotFoundException {
        final String layout =
                "<LinearLayout " +
                        "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "<com.android.layoutlib.bridge.test.widgets.HookWidget " +
                        "              android:layout_width=\"100dp\"\n" +
                        "              android:layout_height=\"200dp\" />\n" +
                        "<LinearLayout " +
                        "              android:background=\"#CBBAF0\"\n" +
                        "              android:layout_width=\"100dp\"\n" +
                        "              android:layout_height=\"200dp\" />\n" +
                        "</LinearLayout>";
        {
            com.android.layoutlib.bridge.test.widgets.HookWidget.setOnPreDrawHook(() -> {
                throw new NullPointerException();
            });

            // Create the layout pull parser.
            LayoutPullParser parser = LayoutPullParser.createFromString(layout);
            // Create LayoutLibCallback.
            LayoutLibTestCallback layoutLibCallback =
                    new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
            layoutLibCallback.initResources();

            SessionParams params = getSessionParamsBuilder()
                    .setParser(parser)
                    .setCallback(layoutLibCallback)
                    .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                    .setRenderingMode(RenderingMode.V_SCROLL)
                    .build();

            renderAndVerify(params, "ondraw_crash.png", TimeUnit.SECONDS.toNanos(2));
        }

        com.android.layoutlib.bridge.test.widgets.HookWidget.reset();

        {
            com.android.layoutlib.bridge.test.widgets.HookWidget.setOnPreMeasure(() -> {
                throw new NullPointerException();
            });

            LayoutPullParser parser = LayoutPullParser.createFromString(layout);
            LayoutLibTestCallback layoutLibCallback =
                    new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
            layoutLibCallback.initResources();

            SessionParams params = getSessionParamsBuilder()
                    .setParser(parser)
                    .setCallback(layoutLibCallback)
                    .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                    .setRenderingMode(RenderingMode.V_SCROLL)
                    .build();

            renderAndVerify(params, "onmeasure_crash.png", TimeUnit.SECONDS.toNanos(2));
        }

        // We expect the view error messages. Fail for anything else.
        sRenderMessages.removeIf(message -> message.equals("View draw failed"));
        sRenderMessages.removeIf(message -> message.equals("View measure failed"));
    }

    /**
     * Paints the borders of the given {@link ViewInfo} and its children to the passed
     * {@link Graphics2D} context.
     * The method will used the given parentLeft and parentTop as the given vInfo coordinates.
     * The depth is used to calculate different colors for the borders depending on the hierarchy
     * depth.
     */
    private void paintBorders(@NonNull Graphics2D g, int parentLeft, int parentTop, int depth,
            @NonNull ViewInfo vInfo) {
        int leftMargin = Math.max(0, vInfo.getLeftMargin());
        int topMargin = Math.max(0, vInfo.getTopMargin());
        int x = parentLeft + vInfo.getLeft() + leftMargin;
        int y = parentTop + vInfo.getTop() + topMargin;
        int w = vInfo.getRight() - vInfo.getLeft();
        int h = vInfo.getBottom() - vInfo.getTop();
        g.setXORMode(java.awt.Color.decode(Integer.toString(depth * 50000)));
        g.drawRect(x, y, w, h);

        for (ViewInfo child : vInfo.getChildren()) {
            paintBorders(g, x, y, depth + 1, child);
        }
    }

    @Test
    public void testViewBoundariesReporting() throws Exception {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\"\n" +
                        "              android:background=\"@drawable/ninepatch\"\n" +
                        "              android:layout_margin=\"20dp\"\n" +
                        "              android:orientation=\"vertical\">\n" + "\n" +
                        "    <TextView\n" +
                        "        android:layout_width=\"150dp\"\n" +
                        "        android:layout_height=\"50dp\"\n" +
                        "        android:background=\"#FF0\"/>\n" +
                        "    <TextView\n" +
                        "        android:layout_width=\"150dp\"\n" +
                        "        android:layout_height=\"50dp\"\n" +
                        "        android:background=\"#F00\"/>\n" +
                        "    <LinearLayout\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:paddingLeft=\"10dp\">\n" +
                        "        <TextView\n" +
                        "            android:layout_width=\"150dp\"\n" +
                        "            android:layout_height=\"50dp\"\n" +
                        "            android:background=\"#00F\"/>\n" +
                        "    </LinearLayout>\n" +
                        "    <LinearLayout\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:layout_marginLeft=\"30dp\"\n" +
                        "        android:layout_marginTop=\"15dp\">\n" +
                        "        <TextView\n" +
                        "            android:layout_width=\"150dp\"\n" +
                        "            android:layout_height=\"50dp\"\n" +
                        "            android:background=\"#F0F\"/>\n" +
                        "    </LinearLayout>\n" +
                        "</LinearLayout>";

        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        RenderResult result = render(sBridge, params, -1);
        BufferedImage image = result.getImage();
        assertNotNull(image);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setStroke(new BasicStroke(8));
        for (ViewInfo vInfo : result.getSystemViews()) {
            paintBorders(g, 0, 0, 0, vInfo);
        }

        verify("view_boundaries.png", image);
    }

    /**
     * Test rendering of strings that have mixed RTL and LTR scripts.
     * <p>
     * http://b/37510906
     */
    @Test
    public void testMixedRtlLtrRendering() throws Exception {
        //
        final String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\"\n" +
                        "              android:orientation=\"vertical\">\n" + "\n" +
                        "    <TextView\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:textSize=\"30sp\"\n" +
                        "        android:background=\"#55FF0000\"\n" +
                        "        android:text=\"این یک رشته ایرانی است\"/>\n" +
                        "    <TextView\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:textSize=\"30sp\"\n" +
                        "        android:background=\"#55FF00FF\"\n" +
                        "        android:text=\"این یک رشته ایرانی است(\"/>\n" +
                        "    <TextView\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:textSize=\"30sp\"\n" +
                        "        android:background=\"#55FAF012\"\n" +
                        "        android:text=\")(این یک رشته ایرانی است(\"/>\n" +
                        "</LinearLayout>";

        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "rtl_ltr.png", -1);
    }

    @Test
    public void testViewStub() throws Exception {
        //
        final String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\"\n" +
                        "              android:orientation=\"vertical\">\n" + "\n" +
                        "      <ViewStub\n" +
                        "        xmlns:tools=\"http://schemas.android.com/tools\"\n" +
                        "        android:layout_width=\"match_parent\"\n" +
                        "        android:layout_height=\"match_parent\"\n" +
                        "        android:layout=\"@layout/four_corners\"\n" +
                        "        tools:visibility=\"visible\" />" +
                        "</LinearLayout>";

        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);

        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "view_stub.png", -1);
    }

    @Test
    public void testImageResize() throws ClassNotFoundException {
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "    android:layout_width=\"match_parent\"\n" +
                        "    android:layout_height=\"match_parent\"\n" +
                        "    android:background=\"@drawable/ninepatch\"\n" +
                        "    android:layout_margin=\"20dp\"\n" +
                        "    android:orientation=\"vertical\">\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n\n" +
                        "    <Button\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:text=\"Button\" />\n"
                        + "</LinearLayout>");

        // Ask for an image that it's 1/10th the size of the actual device image
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setImageFactory((width, height) ->
                        new BufferedImage(width / 10, height / 10,
                                BufferedImage.TYPE_INT_ARGB))
                .setFlag(RenderParamsFlags.FLAG_KEY_RESULT_IMAGE_AUTO_SCALE, true)
                .build();

        renderAndVerify(params, "auto-scale-image.png");
    }

    @Test
    public void testTranslation() throws ClassNotFoundException, FileNotFoundException {
        RenderResult res = renderAndVerify("translate_test.xml", "translate_test.png", false);
        ViewInfo rootInfo = res.getRootViews().get(0);
        ViewInfo buttonInfo = rootInfo.getChildren().get(0);
        assertEquals(100, buttonInfo.getLeft());
    }

    /**
     * Test a vector drawable that uses trimStart and trimEnd. It also tests all the primitives
     * for vector drawables (lines, moves and cubic and quadratic curves).
     */
    @Test
    public void testCanvas() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <com.android.layoutlib.test.myapplication.widgets.CanvasTestView\n" +
                        "             android:layout_height=\"fill_parent\"\n" +
                        "             android:layout_width=\"fill_parent\"\n" +
                        "             android:src=\"@drawable/android\" />\n" + "\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "canvas.png", TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testTypedArrays() throws ClassNotFoundException, FileNotFoundException {
        renderAndVerify("typed_array.xml", "typed_arrays.png", false);
    }

    /**
     * Tests that the gradients are correctly displayed when using transparent colors
     * and a wide range of offset values.
     * <p/>
     * http://b/112759140
     */
    @Test
    public void testAnimatedVectorDrawableWithColorInterpolator() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"match_parent\"\n" +
                        "             android:layout_width=\"match_parent\"\n" +
                        "             android:src=\"@drawable/avd_color_interpolator\" />\n\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "color_interpolation.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testJustified() throws ClassNotFoundException, FileNotFoundException {
        renderAndVerify("justified_inter_word.xml", "justified_inter_word.png", false);
        renderAndVerify("justified_none.xml", "justified_none.png", false);
    }

    @Test
    public void testManyLineBreaks() throws Exception {
        String layout =
                "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" + "\n" +
                        "    <EditText\n" +
                        "        android:layout_width=\"match_parent\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:fallbackLineSpacing=\"true\"\n" +
                        "        android:text=\"A very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very very very very very very very very " +
                        "very very very very very very very long text\"/>\n" +
                        "</FrameLayout>";

        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "many_line_breaks.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testNinePatchDrawable() throws Exception {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"fill_parent\"\n" +
                        "             android:layout_width=\"fill_parent\"\n" +
                        "             android:src=\"@drawable/ninepatch_drawable\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .disableDecoration()
                .build();

        renderAndVerify(params, "ninepatch_drawable.png");
    }

    @Test
    public void testContentId() throws ClassNotFoundException {
        final String layout =
                "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" + "\n" +
                        "    <com.android.layoutlib.bridge.test.widgets.ContentWidget\n" +
                        "        android:layout_width=\"match_parent\"\n" +
                        "        android:layout_height=\"wrap_content\"/>\n" +
                        "</FrameLayout>";

        {
            // Create the layout pull parser.
            LayoutPullParser parser = LayoutPullParser.createFromString(layout);
            // Create LayoutLibCallback.
            LayoutLibTestCallback layoutLibCallback = new LayoutLibTestCallback(getLogger(),
                    mDefaultClassLoader);
            layoutLibCallback.initResources();

            SessionParams params = getSessionParamsBuilder()
                    .setParser(parser)
                    .setCallback(layoutLibCallback)
                    .build();

            RenderResult result = render(sBridge, params, TimeUnit.SECONDS.toNanos(2));
            BufferedImage image = result.getImage();
            assertNotNull(image);
        }

        {
            // Create the layout pull parser.
            LayoutPullParser parser = LayoutPullParser.createFromString(layout);
            // Create LayoutLibCallback.
            LayoutLibTestCallback layoutLibCallback = new LayoutLibTestCallback(getLogger(),
                    mDefaultClassLoader);
            layoutLibCallback.initResources();

            SessionParams params = getSessionParamsBuilder()
                    .setParser(parser)
                    .setCallback(layoutLibCallback)
                    .disableDecoration()
                    .build();

            RenderResult result = render(sBridge, params, TimeUnit.SECONDS.toNanos(2));
            BufferedImage image = result.getImage();
            assertNotNull(image);
        }
    }

    /**
     * Tests that the TextClock widget renders without error
     * <p/>
     * http://b/150151293
     */
    @Test
    public void testTextClock() throws ClassNotFoundException {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <TextClock\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\"\n" +
                        "             android:text=\"12:34\"" +
                        "             android:textSize=\"18sp\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .disableDecoration()
                .build();

        renderAndVerify(params, "textclock.png");
    }

    @Test
    public void testChangeSize() throws ClassNotFoundException {
        final String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:orientation=\"vertical\"\n" +
                        "              android:layout_width=\"wrap_content\"\n" +
                        "              android:layout_height=\"wrap_content\">\n" +
                        "    <Button\n" +
                        "             android:layout_height=\"50dp\"\n" +
                        "             android:layout_width=\"100dp\"\n" +
                        "             android:text=\"Hello\" />\n" +
                        "</LinearLayout>\n";

        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback = new LayoutLibTestCallback(getLogger(),
                mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setRenderingMode(RenderingMode.SHRINK)
                .disableDecoration()
                .build();

        System_Delegate.setBootTimeNanos(TimeUnit.MILLISECONDS.toNanos(871732800000L));
        System_Delegate.setNanosTime(TimeUnit.MILLISECONDS.toNanos(871732800000L));
        RenderSession session = sBridge.createSession(params);

        try {
            session.setElapsedFrameTimeNanos(TimeUnit.SECONDS.toNanos(2));

            if (!session.getResult().isSuccess()) {
                getLogger().error(session.getResult().getException(),
                        session.getResult().getErrorMessage());
            } else {
                // Render the session with a timeout of 50s.
                Result renderResult = session.render(50000);
                if (!renderResult.isSuccess()) {
                    getLogger().error(session.getResult().getException(),
                            session.getResult().getErrorMessage());
                }
            }

            BufferedImage resultImage = session.getImage();

            assertNotNull(resultImage);
            verify("button_resize.png", resultImage);

            Object viewObject = session.getRootViews().get(0)
                    .getChildren().get(0).getViewObject();

            Button btn = (Button) viewObject;
            btn.setLayoutParams(new LinearLayout.LayoutParams(300, 300));

            Result renderResult = session.render(50000);
            if (!renderResult.isSuccess()) {
                getLogger().error(session.getResult().getException(),
                        session.getResult().getErrorMessage());
            }

            resultImage = session.getImage();

            assertNotNull(resultImage);
            verify("button_resize2.png", resultImage);
        } finally {
            session.dispose();
        }
    }

    /**
     * Tests that theme attributes are not resolved when using Resources_Delegate.obtainAttributes
     * <p/>
     * http://b/175943371
     */
    @Test
    public void testNonStyledResources() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:background=\"#999\"" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <com.android.layoutlib.bridge.test.widgets.CustomImageView\n" +
                        "        android:layout_width=\"100dp\"\n" +
                        "        android:layout_height=\"100dp\"/>\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "non-styled_resources.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testRenderEffect() throws ClassNotFoundException {
        // Create the layout pull parser.
        LayoutPullParser parser = LayoutPullParser.createFromString(
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:background=\"#999\"" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" +
                        "    <com.android.layoutlib.bridge.test.widgets.BlurryImageView\n" +
                        "        android:layout_width=\"100dp\"\n" +
                        "        android:layout_height=\"100dp\"/>\n" +
                        "</LinearLayout>");
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "render_effect.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testDialog() throws Exception {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <com.android.layoutlib.test.myapplication.widgets.DialogView\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "dialog.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testWindowBackgroundWithThemeAttribute() throws Exception {
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <TextView\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\"\n" +
                        "             android:text=\"Hello World!\" />\n" +
                        "</LinearLayout>\n";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("WindowBackgroundTheme", true)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "window_background.png",
                TimeUnit.SECONDS.toNanos(2));
    }

    @Test
    public void testThemedAdaptiveIcon() throws ClassNotFoundException, IOException {
        // Create the layout pull parser.
        String layout =
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:padding=\"16dp\"\n" +
                        "              android:orientation=\"horizontal\"\n" +
                        "              android:layout_width=\"fill_parent\"\n" +
                        "              android:layout_height=\"fill_parent\">\n" +
                        "    <ImageView\n" +
                        "             android:layout_height=\"wrap_content\"\n" +
                        "             android:layout_width=\"wrap_content\"\n" +
                        "             android:src=\"@drawable/adaptive\" />\n" +
                        "</LinearLayout>\n";
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();
        SessionParams params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 " +
                        "22.4 22.4 0 50 0Z");
        renderAndVerify(params, "adaptive_icon_circle.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 " +
                        "22.4 22.4 0 50 0Z");
        params.setFlag(RenderParamsFlags.FLAG_KEY_WALLPAPER_PATH,
                "/com/android/layoutlib/testdata/wallpaper1.webp");
        params.setFlag(RenderParamsFlags.FLAG_KEY_USE_THEMED_ICON, true);
        renderAndVerify(params, "adaptive_icon_dynamic_orange.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 " +
                        "22.4 22.4 0 50 0Z");
        params.setFlag(RenderParamsFlags.FLAG_KEY_WALLPAPER_PATH,
                "/com/android/layoutlib/testdata/wallpaper2.webp");
        params.setFlag(RenderParamsFlags.FLAG_KEY_USE_THEMED_ICON, true);
        renderAndVerify(params, "adaptive_icon_dynamic_green.png");

        params = getSessionParamsBuilder()
                .setParser(LayoutPullParser.createFromString(layout))
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .build();
        params.setFlag(RenderParamsFlags.FLAG_KEY_ADAPTIVE_ICON_MASK_PATH,
                "M50 0C77.6 0 100 22.4 100 50C100 77.6 77.6 100 50 100C22.4 100 0 77.6 0 50C0 " +
                        "22.4 22.4 0 50 0Z");
        params.setFlag(RenderParamsFlags.FLAG_KEY_WALLPAPER_PATH,
                "/com/android/layoutlib/testdata/wallpaper2.webp");
        params.setFlag(RenderParamsFlags.FLAG_KEY_USE_THEMED_ICON, false);
        renderAndVerify(params, "adaptive_icon_circle.png");
    }

    @Test
    public void testHtmlText() throws ClassNotFoundException {
        final String layout =
                "<FrameLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                        "              android:layout_width=\"match_parent\"\n" +
                        "              android:layout_height=\"match_parent\">\n" + "\n" +
                        "    <com.android.layoutlib.bridge.test.widgets.HtmlTextView\n" +
                        "        android:layout_width=\"wrap_content\"\n" +
                        "        android:layout_height=\"wrap_content\"\n" +
                        "        android:textSize=\"30sp\"/>\n" +
                        "</FrameLayout>";
        LayoutPullParser parser = LayoutPullParser.createFromString(layout);
        // Create LayoutLibCallback.
        LayoutLibTestCallback layoutLibCallback =
                new LayoutLibTestCallback(getLogger(), mDefaultClassLoader);
        layoutLibCallback.initResources();

        SessionParams params = getSessionParamsBuilder()
                .setParser(parser)
                .setCallback(layoutLibCallback)
                .setTheme("Theme.Material.Light.NoActionBar.Fullscreen", false)
                .setRenderingMode(RenderingMode.V_SCROLL)
                .disableDecoration()
                .build();

        renderAndVerify(params, "html.png", TimeUnit.SECONDS.toNanos(2));
    }
}
