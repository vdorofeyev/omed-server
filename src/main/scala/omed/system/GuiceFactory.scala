package omed.system

import com.google.inject.Guice
import com.google.inject.Injector
import omed.TimerBF.ExampleModule

object GuiceFactory
{
  private val inj: Injector=  Guice.createInjector(new SingletonServletModule())

  def getInjector:Injector=
  {
    inj
  }
}