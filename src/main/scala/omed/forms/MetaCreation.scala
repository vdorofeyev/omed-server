package omed.forms

import java.sql.ResultSet
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 03.04.14
 * Time: 12:06
 * To change this template use File | Settings | File Templates.
 */
/**
 * интерфейс для кеширования  Метаданных
 * @tparam A
 */
trait MetaCreation[A <:AnyRef] {
  def  apply(dbResult:ResultSet):A
  def  query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String
  /**
   * определеятся в beanах если требуется кешировать с группировкой (например поля сгруппированые по классу)
   * @return
   */
  def  groupValue: A => String = null
  /**
   * определеятся в beanах если требуется кешировать отдельно объекты
   * @return
   */
  def  idValue: A => String =  null
  def  createSeqObj(data:Seq[A]):MetaDataSeq[A] = null
  def  storedObjectClass :Class[_ <: AnyRef] = null
  def  storedSeqClass :Class[_ <: AnyRef] = null
}
