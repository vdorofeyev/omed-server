package omed.TimerBF

import com.google.inject.{Injector, Singleton, AbstractModule}
import omed.data.DataReaderService
import com.google.inject.servlet.RequestScoped
import omed.model.MetaObjectCacheManager
import omed.cache.{CommonCacheService, DomainCacheService}
import scala.Singleton
import omed.reports.{TemplateServiceImpl, TemplateService}
import javax.servlet.http.HttpServletRequest
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import omed.bf.handlers._
import omed.bf.{ValidationWarningPool, BusinessFunctionThreadPool}

/**
 * Класс для  работы injects в БФ по расписанию (отсутствует RequestScope)
 */
class ExampleModule extends AbstractModule {
  protected def configure {
    bindScope(classOf[ObjectScoped], objectScope)
  //  bind(classOf[omed.db.DataSourceProvider]).in(classOf[com.google.inject.Singleton])
   // bind(classOf[Injector]).in(objectScope)
    bind(classOf[omed.db.ConnectionProvider]).in(objectScope)

    bind(classOf[omed.TimerBF.BFScheduler]).in(objectScope)

    bind(classOf[SystemSessionProvider])
      .to(classOf[SystemSessionProviderImpl]).in(objectScope)
    bind(classOf[HttpServletRequest])
      .to(classOf[SystemServletRequest]).in(objectScope)
    bind(classOf[omed.system.ContextProvider])
      .to(classOf[omed.system.SessionContextProvider]).in(objectScope)

    bind(classOf[omed.forms.MetaFormProvider])
      .to(classOf[omed.forms.MetaFormProviderImpl]).in(objectScope)
    bind(classOf[omed.forms.MetaFormDBProvider])
      .to(classOf[omed.forms.MetaFormDBProviderImpl]).in(objectScope)

    bind(classOf[omed.model.MetaClassProvider])
      .to(classOf[omed.model.MetaClassProviderImpl]).in(objectScope)

    bind(classOf[omed.model.MetaModel])
      .to(classOf[omed.model.LazyMetaModel]).in(objectScope)


    bind(classOf[omed.model.MetaDBProvider])
      .to(classOf[omed.model.MetaDBProviderImpl]).in(objectScope)
    bind(classOf[omed.data.DataWriterService])
      .to(classOf[omed.data.DataWriterServiceImpl]).in(objectScope)

    bind(classOf[omed.data.DataReaderService])
      .to(classOf[omed.data.DataReaderServiceImpl]).in(objectScope)

    bind(classOf[omed.triggers.TriggerService])
      .to(classOf[omed.model.services.TriggerServiceImpl]).in(objectScope)
    bind(classOf[omed.model.services.SystemTriggerProvider])
      .to(classOf[omed.model.services.SystemTriggerProviderImpl]).in(objectScope)
    bind(classOf[omed.triggers.TriggerExecutor])
      .to(classOf[omed.model.services.TriggerExecutorImpl]).in(objectScope)
    bind(classOf[omed.model.services.ExpressionEvaluator]).in(objectScope)

    bind(classOf[omed.auth.Auth])
      .to(classOf[omed.auth.AuthBean]).in(objectScope)
    bind(classOf[omed.auth.PermissionProvider])
      .to(classOf[omed.auth.PermissionProviderImpl]).in(objectScope)
    bind(classOf[omed.auth.PermissionReader])
      .to(classOf[omed.auth.PermissionReaderImpl]).in(objectScope)

    // cache interfaces
    bind(classOf[MetaObjectCacheManager])
      .in(objectScope)
    bind(classOf[DomainCacheService])
      .in(objectScope)
    bind(classOf[CommonCacheService])
      .in(objectScope)

    //Логирование
    bind(classOf[omed.cache.ExecStatProvider])
      .to(classOf[omed.cache.ExecStatProviderImpl]).in(objectScope)
    // работа с данными
    bind(classOf[omed.data.DataAwareConfiguration])
      .in(objectScope)

    bind(classOf[omed.data.EntityFactory])
      .to(classOf[omed.data.EntityFactoryImpl]).in(objectScope)
    bind(classOf[omed.db.DBProvider])
      .to(classOf[omed.db.DBProviderImpl]).in(objectScope)

    // настройки
    bind(classOf[omed.data.SettingsService])
      .to(classOf[omed.data.SettingsServiceImpl]).in(objectScope)

    // бизнес фукнции
    bind(classOf[omed.bf.BusinessFunctionExecutor])
      .to(classOf[omed.bf.BusinessFunctionExecutorImpl]).in(objectScope)
    bind(classOf[omed.bf.FunctionInfoProvider])
      .to(classOf[omed.bf.FunctionInfoProviderImpl]).in(objectScope)
   // bind(classOf[omed.bf.ProcessStateProvider])
  //    .to(classOf[omed.bf.ProcessStateProviderImpl]).in(classOf[com.google.inject.Singleton])

    bind(classOf[omed.model.EntityDataProvider])
      .to(classOf[omed.model.services.EntityDataProviderImpl]).in(objectScope)

    bind(classOf[omed.bf.CloneObjectProvider])
      .to(classOf[omed.bf.CloneObjectProviderImpl]).in(objectScope)
    bind(classOf[omed.bf.DataSetBuilder])
      .to(classOf[omed.bf.DataSetBuilderImpl]).in(objectScope)
  //  bind(classOf[omed.bf.BusinessFunctionLogger])
  //    .to(classOf[omed.bf.BusinessFunctionLoggerImpl]).in(classOf[com.google.inject.Singleton])
    bind(classOf[omed.bf.ServerStepExecutor])
      .to(classOf[omed.bf.ServerStepExecutorImpl]).in(objectScope)
    bind(classOf[omed.bf.ClientResultParser]).in(objectScope)
    bind(classOf[omed.bf.ConfigurationProvider])
      .to(classOf[omed.bf.ExtendedConfiguration]).in(objectScope)

    bind(classOf[omed.bf.FERSyncronizeProvider])
      .to(classOf[omed.bf.FERSyncronizeProviderImpl]).in(objectScope)
    bind(classOf[omed.roi.ROIProvider])
      .to(classOf[omed.roi.ROIProviderImpl]).in(objectScope)
    //валидация
    bind(classOf[omed.validation.ValidationProvider])
      .to(classOf[omed.validation.ValidationProviderImpl]).in(objectScope)

    //уведомления
    bind(classOf[omed.push.PushNotificationService])
      .to(classOf[omed.push.PushNotificationServiceImpl]).in(objectScope)
    bind(classOf[omed.predNotification.PredNotificationProvider])
      .to(classOf[omed.predNotification.PredNotificationProviderImpl]).in(objectScope)
    // отчёты
    bind(classOf[TemplateService])
      .to(classOf[TemplateServiceImpl])
      .in(objectScope)

    bind(classOf[BusinessFunctionThreadPool]).in(objectScope)
    bind(classOf[ValidationWarningPool]).in(objectScope)

    bind(classOf[ExecStoredProcHandler]).in(objectScope)
    bind(classOf[SetAttrValueHandler]).in(objectScope)
    bind(classOf[StateTransitionHandler]).in(objectScope)
    bind(classOf[SetValueHandler]).in(objectScope)
    bind(classOf[CallAPIHandler]).in(objectScope)
    bind(classOf[CreateObjectHandler]).in(objectScope)
    bind(classOf[ArchiveFilesHandler]).in(objectScope)
    bind(classOf[ExecJsHandler]).in(objectScope)
    bind(classOf[CloneObjectHandler]).in(objectScope)
    bind(classOf[CloneArrayHandler]).in(objectScope)
    bind(classOf[GetServerValueHandler]).in(objectScope)
    bind(classOf[CreateDBFHandler]).in(objectScope)
    bind(classOf[CheckECPHandler]).in(objectScope)
    bind(classOf[CallBFHandler]).in(objectScope)
    bind(classOf[CreateByTemplateHandler]).in(objectScope)
    bind(classOf[UpdateNameHandler]).in(objectScope)
  }

  def getObjectScope: GuiceObjectScope = {
    return objectScope
  }

  private var objectScope: GuiceObjectScope = new GuiceObjectScope
}