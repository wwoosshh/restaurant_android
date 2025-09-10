package com.restaurantapp.data.network;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<Context> contextProvider;

  public AuthInterceptor_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(contextProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<Context> contextProvider) {
    return new AuthInterceptor_Factory(contextProvider);
  }

  public static AuthInterceptor newInstance(Context context) {
    return new AuthInterceptor(context);
  }
}
