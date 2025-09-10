package com.restaurantapp.di;

import android.content.Context;
import com.restaurantapp.data.network.AuthInterceptor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class NetworkModule_ProvideAuthInterceptorFactory implements Factory<AuthInterceptor> {
  private final Provider<Context> contextProvider;

  public NetworkModule_ProvideAuthInterceptorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthInterceptor get() {
    return provideAuthInterceptor(contextProvider.get());
  }

  public static NetworkModule_ProvideAuthInterceptorFactory create(
      Provider<Context> contextProvider) {
    return new NetworkModule_ProvideAuthInterceptorFactory(contextProvider);
  }

  public static AuthInterceptor provideAuthInterceptor(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthInterceptor(context));
  }
}
