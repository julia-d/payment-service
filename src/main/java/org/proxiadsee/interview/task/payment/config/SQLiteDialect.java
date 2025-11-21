package org.proxiadsee.interview.task.payment.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

public class SQLiteDialect extends Dialect {

  public SQLiteDialect(DialectResolutionInfo info) {
    super(info);
  }

  @Override
  public IdentityColumnSupport getIdentityColumnSupport() {
    return new SQLiteIdentityColumnSupport();
  }

  @Override
  public boolean supportsInsertReturning() {
    return false;
  }

  @Override
  public boolean supportsValuesList() {
    return false;
  }

  private static class SQLiteIdentityColumnSupport extends IdentityColumnSupportImpl {
    @Override
    public boolean supportsIdentityColumns() {
      return true;
    }

    @Override
    public String getIdentitySelectString(String table, String column, int type) {
      return "select last_insert_rowid()";
    }

    @Override
    public String getIdentityColumnString(int type) {
      return "integer";
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
      return false;
    }
  }
}
