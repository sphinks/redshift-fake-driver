package org.h2.jdbc

import java.sql.{DriverManager, Connection}
import java.util.Properties

import jp.ne.opt.redshiftfake.FakeConnection
import org.h2.Driver

class FakeH2Driver extends Driver {
  import FakeH2Driver._

  override def connect(url: String, info: Properties): Connection =
    new FakeConnection(new JdbcConnection(url.replaceFirst(urlPrefix, "jdbc:h2"), info))

  override def acceptsURL(url: String): Boolean = url.startsWith(urlPrefix)
}

object FakeH2Driver {
  private[this] val driverInstance = new FakeH2Driver
  DriverManager.registerDriver(driverInstance)

  private val urlPrefix = "jdbc:h2redshift"

  def createConnection(url: String, prop: java.util.Properties): Connection = {
    DriverManager.getConnection(url, prop)
  }
}