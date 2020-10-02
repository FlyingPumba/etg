package org.etg.espresso.templates.kotlin;

import org.etg.espresso.templates.VelocityTemplate;

public class MockedServerTest implements VelocityTemplate {

    @Override
    public String getName() {
        return "MockedServerTest.kt";
    }

    @Override
    public String getRelativePath() {
        return "utils/";
    }

    public String getAsRawString() {
        return "package ${PackageName}.utils\n" +
                "\n" +
                "import androidx.test.espresso.IdlingRegistry\n" +
                "import androidx.test.espresso.IdlingResource\n" +
                "import ${PackageName}.di.buildOkHttpClient\n" +
                "import com.jakewharton.espresso.OkHttp3IdlingResource\n" +
                "import com.squareup.rx2.idler.Rx2Idler\n" +
                "import io.reactivex.android.plugins.RxAndroidPlugins\n" +
                "import io.reactivex.plugins.RxJavaPlugins\n" +
                "import okhttp3.logging.HttpLoggingInterceptor\n" +
                "import okhttp3.mockwebserver.MockWebServer\n" +
                "import org.junit.After\n" +
                "import org.junit.Before\n" +
                "import org.koin.core.qualifier.named\n" +
                "import org.koin.test.KoinTest\n" +
                "import org.koin.test.mock.declare\n" +
                "\n" +
                "open class MockedServerTest : KoinTest {\n" +
                "    protected val webServer = MockWebServer()\n" +
                "    protected lateinit var baseOkHttpIdlingResource: IdlingResource\n" +
                "    protected lateinit var staticOkHttpIdlingResource: IdlingResource\n" +
                "\n" +
                "    @Before\n" +
                "    fun setUp() {\n" +
                "        webServer.start()\n" +
                "\n" +
                "        // Redirect base and static URLs to localhost, where the MockServer is running\n" +
                "        declare(named(\"base_url\")) {\n" +
                "            webServer.url(\"/\").toString()\n" +
                "        }\n" +
                "        declare(named(\"static_url\")) {\n" +
                "            webServer.url(\"/\").toString()\n" +
                "        }\n" +
                "\n" +
                "        // Create OkHttpClients here to declare them as Idling Resources\n" +
                "        declare {\n" +
                "            val client = buildOkHttpClient(true, HttpLoggingInterceptor.Level.BODY)\n" +
                "            baseOkHttpIdlingResource = OkHttp3IdlingResource.create(\"OkHttp - base\", client)\n" +
                "            IdlingRegistry.getInstance().register(baseOkHttpIdlingResource)\n" +
                "        }\n" +
                "        declare(named(\"static\")) {\n" +
                "            val client = buildOkHttpClient(false, HttpLoggingInterceptor.Level.BODY)\n" +
                "            staticOkHttpIdlingResource = OkHttp3IdlingResource.create(\"OkHttp - static\", client)\n" +
                "            IdlingRegistry.getInstance().register(staticOkHttpIdlingResource)\n" +
                "        }\n" +
                "\n" +
                "        // Create Rx handlers that will wrap themselves with an Espresso's Idling Resource and\n" +
                "        // register it to the Espresso class.\n" +
                "        RxJavaPlugins.setInitComputationSchedulerHandler(\n" +
                "                Rx2Idler.create(\"RxJava 2.x Computation Scheduler\"))\n" +
                "        RxJavaPlugins.setInitIoSchedulerHandler(\n" +
                "                Rx2Idler.create(\"RxJava 2.x IO Scheduler\"))\n" +
                "        RxAndroidPlugins.setInitMainThreadSchedulerHandler(\n" +
                "                Rx2Idler.create(\"RxAndroid Scheduler\"))\n" +
                "    }\n" +
                "\n" +
                "    @After\n" +
                "    fun tearDown() {\n" +
                "        webServer.shutdown()\n" +
                "        IdlingRegistry.getInstance().unregister(baseOkHttpIdlingResource)\n" +
                "        IdlingRegistry.getInstance().unregister(staticOkHttpIdlingResource)\n" +
                "    }\n" +
                "}";
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof MockedServerTest;
    }

    @Override
    public int hashCode() {
        return getAsRawString().hashCode();
    }


}
