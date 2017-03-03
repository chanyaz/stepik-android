package org.stepic.droid.core.modules;

import org.stepic.droid.analytic.Analytic;
import org.stepic.droid.concurrency.MainHandler;
import org.stepic.droid.core.StepikLogoutManager;
import org.stepic.droid.core.presenters.ProfileMainFeedPresenter;
import org.stepic.droid.preferences.SharedPreferenceHelper;
import org.stepic.droid.web.Api;

import java.util.concurrent.ThreadPoolExecutor;

import dagger.Module;
import dagger.Provides;

@Module
public class MainFeedModule {

    @Provides
    ProfileMainFeedPresenter provideProfileMainFeedPresenter(MainHandler mainHandler,
                                                             ThreadPoolExecutor threadPoolExecutor,
                                                             Analytic analytic,
                                                             SharedPreferenceHelper sharedPreferenceHelper,
                                                             Api api,
                                                             StepikLogoutManager stepikLogoutManager) {
        return new ProfileMainFeedPresenter(sharedPreferenceHelper, mainHandler, api, threadPoolExecutor, analytic, stepikLogoutManager);
    }
}
