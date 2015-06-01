package omed.system

import com.google.inject.{Scopes, Guice, Injector, Singleton}
import com.google.inject.servlet.{SessionScoped, RequestScoped, GuiceServletContextListener}
import com.sun.jersey.guice.JerseyServletModule
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import com.sun.jersey.api.core.{ResourceConfig, PackagesResourceConfig}
import omed.model.MetaObjectCacheManager
import omed.reports.{TemplateServiceImpl, TemplateService}
import omed.cache.{CommonCacheService, DomainCacheService}
import omed.bf.handlers._
import omed.bf.{ValidationWarningPool, BusinessFunctionThreadPool}

/**
 * Конфигурирование сервлета.
 * Подключение конкретных реализация через Guice Dependancy Injection механизм.
 */
class ConfiguredJerseyServletModule extends JerseyServletModule {
  override protected def configureServlets() {
//    bind(classOf[omed.db.DataSourceProvider]).in(classOf[com.google.inject.Singleton])

    bind(classOf[omed.db.ConnectionProvider]).in(classOf[RequestScoped])
    bind(classOf[omed.system.ContextProvider])
      .to(classOf[omed.system.SessionContextProvider]).in(classOf[RequestScoped])

    bind(classOf[omed.forms.MetaFormProvider])
      .to(classOf[omed.forms.MetaFormProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.forms.MetaFormDBProvider])
      .to(classOf[omed.forms.MetaFormDBProviderImpl]).in(classOf[RequestScoped])

    bind(classOf[omed.model.MetaClassProvider])
      .to(classOf[omed.model.MetaClassProviderImpl]).in(classOf[RequestScoped])

    bind(classOf[omed.model.MetaModel])
      .to(classOf[omed.model.LazyMetaModel]).in(classOf[RequestScoped])


    bind(classOf[omed.model.MetaDBProvider])
      .to(classOf[omed.model.MetaDBProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.data.DataWriterService])
      .to(classOf[omed.data.DataWriterServiceImpl]).in(classOf[RequestScoped])

    bind(classOf[omed.data.DataReaderService])
      .to(classOf[omed.data.DataReaderServiceImpl]).in(classOf[RequestScoped])

    bind(classOf[omed.triggers.TriggerService])
      .to(classOf[omed.model.services.TriggerServiceImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.model.services.SystemTriggerProvider])
      .to(classOf[omed.model.services.SystemTriggerProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.triggers.TriggerExecutor])
      .to(classOf[omed.model.services.TriggerExecutorImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.model.services.ExpressionEvaluator]).in(classOf[RequestScoped])

    bind(classOf[omed.auth.Auth])
      .to(classOf[omed.auth.AuthBean]).in(classOf[RequestScoped])
    bind(classOf[omed.auth.PermissionProvider])
      .to(classOf[omed.auth.PermissionProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.auth.PermissionReader])
      .to(classOf[omed.auth.PermissionReaderImpl]).in(classOf[RequestScoped])

    // cache interfaces
    bind(classOf[MetaObjectCacheManager])
      .in(classOf[RequestScoped])
    bind(classOf[DomainCacheService])
      .in(classOf[RequestScoped])
    bind(classOf[CommonCacheService])
      .in(classOf[RequestScoped])

    //Логирование
    bind(classOf[omed.cache.ExecStatProvider])
      .to(classOf[omed.cache.ExecStatProviderImpl]).in(classOf[RequestScoped])
    // работа с данными
    bind(classOf[omed.data.DataAwareConfiguration])
      .in(classOf[RequestScoped])

    bind(classOf[omed.data.EntityFactory])
      .to(classOf[omed.data.EntityFactoryImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.db.DBProvider])
      .to(classOf[omed.db.DBProviderImpl]).in(classOf[RequestScoped])

    // настройки
    bind(classOf[omed.data.SettingsService])
      .to(classOf[omed.data.SettingsServiceImpl]).in(classOf[RequestScoped])

    // бизнес фукнции
    bind(classOf[omed.bf.BusinessFunctionExecutor])
      .to(classOf[omed.bf.BusinessFunctionExecutorImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.bf.FunctionInfoProvider])
      .to(classOf[omed.bf.FunctionInfoProviderImpl]).in(classOf[RequestScoped])
 //   bind(classOf[omed.bf.ProcessStateProvider])
 //     .to(classOf[omed.bf.ProcessStateProviderImpl]).in(classOf[Singleton])

    bind(classOf[omed.model.EntityDataProvider])
      .to(classOf[omed.model.services.EntityDataProviderImpl]).in(classOf[RequestScoped])

    bind(classOf[omed.bf.CloneObjectProvider])
    .to(classOf[omed.bf.CloneObjectProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.bf.DataSetBuilder])
    .to(classOf[omed.bf.DataSetBuilderImpl]).in(classOf[RequestScoped])
  //  bind(classOf[omed.bf.BusinessFunctionLogger])
   //   .to(classOf[omed.bf.BusinessFunctionLoggerImpl]).in(classOf[Singleton])
    bind(classOf[omed.bf.ServerStepExecutor])
      .to(classOf[omed.bf.ServerStepExecutorImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.bf.ClientResultParser]).in(classOf[RequestScoped])
    bind(classOf[omed.bf.ConfigurationProvider])
      .to(classOf[omed.bf.ExtendedConfiguration]).in(classOf[RequestScoped])

    bind(classOf[omed.bf.FERSyncronizeProvider])
      .to(classOf[omed.bf.FERSyncronizeProviderImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.roi.ROIProvider])
      .to(classOf[omed.roi.ROIProviderImpl]).in(classOf[RequestScoped])
    //валидация
    bind(classOf[omed.validation.ValidationProvider])
    .to(classOf[omed.validation.ValidationProviderImpl]).in(classOf[RequestScoped])

    //уведомления
    bind(classOf[omed.push.PushNotificationService])
      .to(classOf[omed.push.PushNotificationServiceImpl]).in(classOf[RequestScoped])
    bind(classOf[omed.predNotification.PredNotificationProvider])
      .to(classOf[omed.predNotification.PredNotificationProviderImpl]).in(classOf[RequestScoped])
    // отчёты
    bind(classOf[TemplateService])
      .to(classOf[TemplateServiceImpl])
      .in(classOf[RequestScoped])

    bind(classOf[BusinessFunctionThreadPool]).in(classOf[RequestScoped])
    bind(classOf[ValidationWarningPool]).in(classOf[RequestScoped])

    bind(classOf[ExecStoredProcHandler]).in(classOf[RequestScoped])
    bind(classOf[SetAttrValueHandler]).in(classOf[RequestScoped])
    bind(classOf[StateTransitionHandler]).in(classOf[RequestScoped])
    bind(classOf[SetValueHandler]).in(classOf[RequestScoped])
    bind(classOf[CallAPIHandler]).in(classOf[RequestScoped])
    bind(classOf[CreateObjectHandler]).in(classOf[RequestScoped])
    bind(classOf[ArchiveFilesHandler]).in(classOf[RequestScoped])
    bind(classOf[ExecJsHandler]).in(classOf[RequestScoped])
    bind(classOf[CloneObjectHandler]).in(classOf[RequestScoped])
    bind(classOf[CloneArrayHandler]).in(classOf[RequestScoped])
    bind(classOf[GetServerValueHandler]).in(classOf[RequestScoped])
    bind(classOf[CreateDBFHandler]).in(classOf[RequestScoped])
    bind(classOf[CheckECPHandler]).in(classOf[RequestScoped])
    bind(classOf[CallBFHandler]).in(classOf[RequestScoped])
    bind(classOf[CreateByTemplateHandler]).in(classOf[RequestScoped])
    bind(classOf[UpdateNameHandler]).in(classOf[RequestScoped])

    // точки доступа, фильтры и провайдеры подключаются явно вместе с пакетом
    import scala.collection.JavaConversions._
    val params = Map(
      PackagesResourceConfig.PROPERTY_PACKAGES ->
        "omed.rest",
      ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS ->
        ("omed.rest.filters.AuthRequestFilter;" +
          "omed.rest.filters.AccessLogRequestFilter")
    )
    serve("/api/*").`with`(classOf[GuiceContainer], params)
    binder().bind(classOf[GuiceContainer]).asEagerSingleton()
  }
}

/**
 * Создание фильтра конктекста для подключения инжектора ConfiguredJerseyServletModule.
 */
class GuiceServletConfig extends GuiceServletContextListener {
  override protected def getInjector(): Injector = {
    return GuiceFactory.getInjector.createChildInjector(new ConfiguredJerseyServletModule())
  }
}
