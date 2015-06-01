package omed

class LoadableMockResultSet extends MockResultSet {

  /**
   * Загрузить данные в resultSet
   */
  def loadData(columns: List[String], data: List[List[Object]]): java.sql.ResultSet = {
    
    val newRs = new MockResultSet
    
    //загрузить колонки
    columns.foreach(column =>
      newRs.columnMap.put(column, columns.indexWhere((_ == column))))
    // загрузить данные
    data.foreach(row => newRs.rowset.add(row.toArray))
    
    newRs
  }

}