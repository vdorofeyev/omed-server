package omed.errors

/**
 * Created by andrejnaryskin on 06.03.14.
 */
class DataAccessError (val recordId:String)
  extends Exception("Нет прав для доступа к объекту: "+recordId) {}
