package org.proxiadsee.interview.task.payment.config;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

public class SQLiteDialect extends Dialect {

  public SQLiteDialect() {
    super();
  }

  public IdentityColumnSupport getIdentityColumnSupport() {
    return new SQLiteIdentityColumnSupport();
  }

  public boolean supportsJdbcGeneratedKeys() {
    // SQLite JDBC driver does not implement getGeneratedKeys reliably
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
      // Use INTEGER for AUTOINCREMENT primary keys in SQLite
      return "integer";
    }
  }
}
