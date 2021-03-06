package jp.ne.opt.redshiftfake.parse

import org.scalatest.FlatSpec

class DDLParserTest extends FlatSpec {
  it should "sanitize Redshift specific DDL" in {
    val ddl =
      """
        |CREATE TABLE foo_bar(a int ENCODE ZSTD, b boolean)
        |DISTSTYLE ALL
        |DISTKEY(a)
        |INTERLEAVED SORTKEY(a, b);
        |""".stripMargin

    val expected =
      """
        |CREATE TABLE foo_bar(a int, b boolean)
        |
        |
        |;
        |""".stripMargin

    assert(DDLParser.sanitize(ddl) == expected)
  }

  it should "sanitize DDL with quoted identifier" in {
    val ddl =
      """CREATE TEMPORARY TABLE test_table
        |(
        |  "test_identifier" INT NOT NULL,
        |)
        |DISTKEY("test_identifier")
        |SORTKEY("test_identifier");
        |""".stripMargin

    val expected =
      """CREATE TEMPORARY TABLE test_table
        |(
        |  "test_identifier" INT NOT NULL,
        |);
        |""".stripMargin

    assert(DDLParser.sanitize(ddl) == expected)
  }
}
