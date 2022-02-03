# このアプリケーションについて

SpringBootのシンプルなTodoListアプリケーションに、リトライやサーキットブレーカーを実装したデモアプリケーション。
実際のアプリケーションでは、どのレイヤでリトライの実装をすべきかなどを要件にあわせて考慮する必要があるが、ここではシンプルにサービスクラスでリトライを実装している。

##データベースの準備

### Postgresql
```
psql --host [DBサーバFQDN] -U [DBユーザ名] postgres

postgres => create database todoapp;
postgres => todoappに切り替え
todoapp => create table if not exists todo_item (
    id serial,
    title varchar(30),
    description varchar(256),
    finished boolean,
    primary key(id)
);
```

## ビルドと実行

アプリケーションの設定は、application.yaml とそこから参照している application-XXX.yamlにDBサーバなど固有の設定を記載している。
ビルドする前に環境に合わせて、application.yamlを編集する。

[src/main/resources/application.yaml]
```
spring:
  profiles:
    active: pgflex
```

[src/main/resources/application-pgflex.yaml]
```
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_SERVER}:${DB_PORT}/${DB_NAME}
    username: '${DB_USER}'
    password: '${DB_PASSWORD}'
    hikari:
      connection-timeout:  3000 # Default: 30000(30 sec)
      connection-test-query: SELECT 1
      maximum-pool-size: 5
```

### JARの生成
```
mvn clean package
```

### Dockerイメージの作成
マルチステージビルドを利用するようになっているので、docker buildのプロセスでアプリケーショのビルドとコンテナイメージを生成する。
```
docker build . -t [タグ]
```

### ローカルでの実行

1. DBの接続プロパティを環境に合わせて編集  
	- src/main/resources/application.yaml
	- src/main/resources/application-XXX.yaml
2. ビルド(JAR or コンテナイメージの生成)
3. 実行
* JARでの実行(DBサーバの接続プロパティ、ユーザなどな環境に合わせて要変更)
```
export DB_SERVER=pgflex1qazxsw2.postgres.database.azure.com
export DB_PORT=5432
export DB_USER=appuser
export DB_PASSWORD=@ppuser12345678
export DB_NAME=todoapp

java -jar target/spring-todo-app-2.0-SNAPSHOT.jar
```
* Dockerでの実行
```
docker run --rm -p 8080:8080 -e "DB_PORT=5432" -e "DB_USER=myadmin" -e "DB_PASSWORD=@pp12345678" -e "DB_SERVER=pgflex1qazxsw2.postgres.database.azure.com" -e "DB_NAME=todoapp" [コンテナイメージ名]
```
4. クライアントからの接続
* アプリケーションは8080ポートでリクエストを待ち受けるのでブラウザから`localhost:8080`にアクセス
* APIを直接利用する場合  
Visual Studio CodeのRest Clientエクステンションを利用する場合には、以下のようなリクエストを送る
```
###
POST http://localhost:8080/api/todolist
Content-Type: application/json

{
    "title": "TTT",
    "description": "DDD"
}

### update
PUT http://localhost:8080/api/todolist
Content-Type: application/json

{
  "id": "2",
  "title": "aaaaa2",
  "description": "DD2",
  "finished": false
}


### get all

GET http://localhost:8080/api/todolist
Content-Type: application/json


###
GET http://localhost:8080/api/todolist/1
Content-Type: application/html



### delete
DELETE http://localhost:8080/api/todolist/1
Content-Type: application/html
```


// ### AKS


## リトライの実装

データのCRUD処理をするサービスクラス `com.microsoft.springframework.samples.service.springretry.TodoServiceImpl` クラスはSpring Retry でリトライを実装し、`com.microsoft.springframework.samples.service.r4j.TodoServiceImpl` クラスはresilience4jでリトライとサーキットブレーカーを実装。
どちらのサービスクラスを利用するかは、`com.microsoft.springframework.samples.controller.TodoApplicationController`クラスで切り替える。


### Resilience4j
Circuit Breaker, Retry, Bulkhead の機能を提供している。
これらの機能のアスペクトは、Function > Bulkhead > TimeLimiter > RateLimiter > CircuitBreaker > Retry の順に評価される。リトライが終わってから左ーキットブレーカーを発動したい場合にはAspectOrderを次のように設定する。

```
resilience4j:
  circuitbreaker:
    circuitBreakerAspectOrder: 1
  retry:
    retryAspectOrder: 2
```

#### Circuit Breaker
count-based sliding window : 最新のN回の呼び出し結果を集計
time-based sliding window : 最新のN秒間の呼び出し結果を集計

サーキットブレーカーがCLOSEDからOPENになる状況の例は、
- 呼び出しの失敗の割合≧成功の割合
- 5秒以上かかる呼び出し≧50%
など。

サーキットの状態遷移

CLOSE -> OPEN 失敗や遅い呼び出しレートが高い
OPEN -> HALF OPEN 一定時間経過
HALF OPEN -> OPEN 失敗や遅い呼び出しレートが高い
HALF OPEN -> CLOSE
OPENの場合は、CallNotPermittedExceptionが発生
[image/CircuitBreakerStateMachine.jpg]

DISABLED　いつも呼び出し可能
FORCED_OPEN いつも呼び出し拒否

|プロパティ|デフォルト値|説明|
|---|---|---|
|failureRateThreashold|50|失敗レートの閾値。この閾値以上に失敗した場合は、サーキットがOPENになる|
|slowCallRateThreahold|100|時間のかかる呼び出しレートの閾値。slowCallDurationThresholdよりも時間がかかる場合は、時間のかかる呼び出しとカウントされる|
|slowCallDurationThreshold|60000 ms60秒)|遅い呼び出しと判断する閾値|
|permittedNumberOfCallsInHalfOpenState|10|サーキットがHALF OPENのときに呼び出し可能な回数|
|maxWaitDurationInHalfOpenState|0 ms|サーキットがOPENになるまでにHALF OPENの状態を継続できる最大の時間|
|slidingWindowType|COUNT_BASED|スライディングウィンドウのタイプ、COUNT_BASED か　TIME_BASEDが選択可能|
|slidingWindowSize|100|スライディングウィンドウのサイズ|
|minimumNumberOfCalls|100|スライディングウィンドウごとの最小の呼び出し回数。例えば、この値が10の場合には9回コールに失敗してもサーキットはOPENにならない|
|waitDurationInOpenState|60000 ms|サーキットがOPENからHALF OPENになるまでの時間|
|automaticTransitionFromOpenToHaflOpenEnabled|false|trueの場合は、サーキットがOPENからHALF OPENに自動的に遷移|
|recordExceptions|empty|記録するExceptionのリスト|
|ignoreExceptions|empty|無視するExceptionのリスト|
|recordFailurePredicate| Throwable （デフォルトではすべての例外を失敗として扱う）|失敗と判断する例外|

### 参考
 https://spring.pleiades.io/spring-cloud-circuitbreaker/docs/current/reference/html/#configuring-resilience4j-circuit-breakers
https://resilience4j.readme.io/docs/getting-started