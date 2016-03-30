package com.angcyo.pldroiddemo;

import android.app.Application;
import android.test.ApplicationTestCase;
import android.test.UiThreadTest;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        assertFalse(false);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @UiThreadTest
    public void demo() {

    }

    public void testDemo() {
        
    }
}
