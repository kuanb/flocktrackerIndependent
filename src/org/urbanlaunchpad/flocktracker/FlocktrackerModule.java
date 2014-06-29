package org.urbanlaunchpad.flocktracker;

import android.content.Context;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import org.urbanlaunchpad.flocktracker.fragments.HubPageFragment;
import org.urbanlaunchpad.flocktracker.fragments.StatisticsPageFragment;
import org.urbanlaunchpad.flocktracker.helpers.SubmissionHelper;
import org.urbanlaunchpad.flocktracker.models.Metadata;
import org.urbanlaunchpad.flocktracker.models.Statistics;

import javax.inject.Singleton;

@Module(
    injects = {
        SurveyorActivity.class,
        HubPageFragment.class,
        StatisticsPageFragment.class
    }
)
public class FlocktrackerModule {
  private final SurveyorActivity activity;

  public FlocktrackerModule(SurveyorActivity activity) {
    this.activity = activity;
  }

  @Provides @Singleton
  Context provideContext() {
    return activity;
  }

  @Provides @Singleton
  SubmissionHelper provideSubmissionHelper() {
    return new SubmissionHelper();
  }

  @Provides @Singleton
  Bus provideEventBus() {
    return new Bus();
  }

  @Provides @Singleton
  Metadata provideMetadata() {
    return new Metadata();
  }

  @Provides @Singleton
  Statistics provideStatistics(Context context, Metadata metadata) {
    return new Statistics(context, metadata);
  }
}
