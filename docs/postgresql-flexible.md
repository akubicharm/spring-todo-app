# Azure Database for Postgresql Flexible Serverの設定

## アプリケーションのデータソース設定

プライマリをWriteだけで行うように targetServerType=primaryとする

```
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://pgflex1qazxsw2primary.postgres.database.azure.com:5432,pgflex1qazxsw2replica.postgres.database.azure.com:5432/todoapp?targetServerType=primary
    username: 'pgadmin'
    password: '@dmin12345678'
    hikari:
      connection-timeout:  3000 # Default: 30000(30 sec)
      connection-test-query: SELECT 1
      maximum-pool-size: 5
```


## 論理レプリケーションの設定

2022/2 時点では、まだRead Replicaの機能がGAされていないので、FlexibleServer 2台構成でLogical Replication構成にして試すための設定方法。

### パブリッシャーの設定　（プライマリ側）
|サーバパラメータ名|設定値|説明|
|---|---|---|
|wal_level|logical|Flexible ServerのデフォルトはREPLICAなのでLOGICALに変更|
|max_replication_slots|(レプリカ数+予備)|Flexibler Serverのデフォルトが10なので変更なし|
|max_wal_sencer|(同時接続する物理レプリカ数）|Flexibler Serverのデフォルトが10なので変更なし|


* 管理者ユーザのレプリケーションのアクセス許可を付与
```
ALTER ROLE <adminname> WITH REPLICATION;
```

* テーブルのパブリケーション作成
```
CREATE PUBLICATION mypub FOR TABLE todo_item;
```

### サブスクライバーの設定　（レプリカ側）
|サーバパラメータ名|設定値|説明|
|---|---|---|
|max_replication_slots|(サブスクライバー数+予備)|Flexibler Serverのデフォルトが10なので変更なし|
|max_logical_replication_workers|（サブスクリプション数+予備）|Flexible Serverのデフォルトが4なので変更なし|
|max_worker_processes|16 (max_logical_replication_workers +1)|https://docs.microsoft.com/ja-jp/azure/postgresql/flexible-server/concepts-logical の記述にしたがいデフォルトの8から変更|

* 管理者ユーザのレプリケーションのアクセス許可を付与
```
ALTER ROLE <adminname> WITH REPLICATION;
```

* テーブルのサブスクライブの作成
```
CREATE SUBSCRIPTION mysub CONNECTION 'host=pgflex1qazxsw2primary.postgres.database.azure.com port=5432 dbname=todoapp user=pgadmin password=@dmin12345678 sslmode=require' PUBLICATION mypub;
```
　※接続設定のオプションは、AzureポータルのConnection stringのセクションで確認できる




## 参考
https://www.postgresql.jp/document/13/html/logical-replication-config.html