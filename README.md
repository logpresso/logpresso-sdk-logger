# 로그프레소 수집기 SDK

## 개요

로그프레소 수집기는 로그프레소 서버 혹은 로그프레소 센트리에 설치되어 데이터를 수집합니다. 로그프레소 엔터프라이즈는 SYSLOG, SNMP, JDBC, FTP, SFTP, JMX, JMS 등 다양한 네트워크 프로토콜을 내장 지원하지만, 서드파티 제품을 연동할 때 특정한 SDK 혹은 네이티브 API를 호출해야 하는 상황이라면 커스텀 수집기를 개발할 필요가 있습니다.

이 저장소는 아래와 같이 구성됩니다:
* **logpresso-logger-example**: 랜덤 값을 수집하는 예제 코드를 통해 기본적인 수집기 구조를 설명합니다.

## 기본 구조
로그프레소의 수집기는 araqne-log-api 번들에 정의된 LoggerFactory와 Logger 인터페이스를 구현합니다. LoggerFactory 인터페이스는 수집기를 구성하는데 필요한 메타데이터를 정의하며, Logger 인터페이스는 실질적인 수집 동작을 정의합니다. 개발자는 각각 AbstractLoggerFactory 클래스와 AbstractLogger 클래스를 상속하여 필수적인 부분만 구현하면 됩니다.

AbstractLoggerFactory 클래스 상속 후 구현해야 할 항목은 아래와 같습니다:
* String getName()
 * 기존의 수집기와 겹치지 않은 유일한 수집기 팩토리 이름을 반환합니다.
* String getDisplayName(Locale locale)
 * 사용자 설정 화면에 출력할 수집기 유형 이름을 반환합니다. locale 매개변수는 현재 사용자 세션의 로케일이므로, 이에 맞는 텍스트를 반환합니다.
* String getDescription(Locale locale)
 * 사용자 설명 화면에 출력할 수집기 설명을 반환합니다. locale 매개변수는 현재 사용자 세션의 로케일이므로, 이에 맞는 텍스트를 반환합니다.
* Collection<LoggerConfigOption> getConfigOptions()
 * 수집기를 구성하는데 필요한 설정 명세 목록을 반환합니다. 로그프레소는 반환되는 LoggerConfigOption에 따라 자동으로 설정 화면을 구성하여 출력합니다.
* Logger createLogger(LoggerSpecification spec)
 * 주어진 수집기 명세를 이용하여 수집기 인스턴스를 생성합니다.

AbstractLogger 클래스 상속 후 구현해야 할 항목은 아래와 같습니다:
* void runOnce()
 * 이 콜백 메소드는 수집주기마다 호출됩니다. runOnce() 실행 도중 예외가 발생하는 경우, 일시적인 수집 실패라면 setTemporaryFailure() 메소드를 이용하여 예외 정보를 설정해야 합니다. 예외를 runOnce() 밖으로 던지면, 의도하지 않은 예외로 간주하여 수집기가 자동으로 정지됩니다.
* boolean isPassive()
 * 만약, 별도의 수집주기 없이 수동적으로 데이터를 전달받는다면, isPassive() 메소드를 오버라이드하여 true를 반환해야 합니다. 예를 들어, 시스로그 수집기는 UDP 포트를 열고 패킷이 수신될 때마다 즉시 수집하므로, 별도의 수집주기를 가지지 않습니다.
* void onStart(LoggerStartReason reason)
 * 수집기가 시작될 때 onStart() 콜백이 호출됩니다. 이 콜백을 이용하여 수집기 초기화를 수행할 수 있습니다.
* void onStop(LoggerStopReason reason)
 * 수집기가 정지될 때 onStop() 콜백이 호출됩니다. 이 콜백을 이용하여 수집기 자원 정리를 수행할 수 있습니다.

LoggerFactory는 OSGi 서비스로 등록되어야 합니다. 로그프레소 수집기는 일반적으로 iPOJO 컴포넌트로 구현하므로 LoggerFactory 클래스의 상단에 @Component, @Provides 어노테이션을 지정합니다. iPOJO 컴포넌트의 인스턴스를 생성하려면 metadata.xml 파일 혹은 @Instantiate 어노테이션을 설정하여야 합니다.

## 예제 코드

아래 예제 코드는 매 수집주기마다 임의의 난수를 수집하는 수집기의 구현입니다.

### RandomLoggerFactory 코드
```
// 기본 구현이 되어있는 AbstractLoggerFactory 클래스를 상속합니다.
// @Component 및 @Provides 선언을 해야 OSGi 서비스로 등록되어 정상적으로 동작합니다.
@Component(name = "random-logger-factory")
@Provides
public class RandomLoggerFactory extends AbstractLoggerFactory {
	// 수집기 유형 고유 식별자를 반환합니다.
	@Override
	public String getName() {
		return "random";
	}

	// 주어진 로케일에 맞는 수집기 유형 이름을 반환합니다.
	@Override
	public String getDisplayName(Locale locale) {
		return "Random";
	}

	// 주어진 로케일에 맞는 설명 텍스트를 반환합니다.
	@Override
	public String getDescription(Locale locale) {
		return "Random number logger";
	}

	// 수집기에 대한 설정 명세 목록을 반환합니다.
	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		// repeat 설정으로 실행 주기마다 몇 개의 난수를 생성할지 입력받습니다.
		LoggerConfigOption repeat = new IntegerConfigType("repeat", t("Repeat count"), t("Generated logs per iteration"), false);
		return Arrays.asList(repeat);
	}

	private Map<Locale, String> t(String en) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		return m;
	}

	// 주어진 명세로 수집기 인스턴스를 생성하여 반환합니다.
	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		return new RandomLogger(spec, this);
	}
}
```

### RandomLogger 코드
```
// 기본 구현이 되어있는 AbstractLogger 클래스를 상속합니다.
public class RandomLogger extends AbstractLogger {

	public RandomLogger(LoggerSpecification spec, LoggerFactory factory) {
		super(spec, factory);
	}

	// 지정된 주기마다 매번 runOnce() 메소드가 호출됩니다.
	@Override
	protected void runOnce() {
		int repeat = 1;
		if (getConfig().get("repeat") != null)
			repeat = Integer.parseInt(getConfig().get("repeat"));

		for (int i = 0; i < repeat; i++) {
			long num = new Random().nextLong();
			String line = "random number: " + Long.toString(num);

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("line", line);

			// SimpleLog 생성자의 첫번째 인자는 로그 타임스탬프입니다.
			// 과거 시점의 로그를 인식시킨다면 날짜를 파싱하여 사용해야 합니다.
			// 두번째 인자는 로거의 이름 (이름공간\이름 형식)이며, 
			// 세번째 인자는 실제 데이터를 포함하는 연관 배열(Map)입니다.
			Log log = new SimpleLog(new Date(), getFullName(), params);

			// write() 메소드를 호출하여 로거가 수집한 로그를 전달합니다.
			// 로그는 트랜스포머를 거쳐 스토리지 및 인덱싱 엔진으로 전달됩니다.
			write(log);
		}
	}
}
```
### metadata.xml 설정
아래와 같이 iPOJO 컴포넌트 인스턴스 생성을 설정합니다. component 특성 값은 로거 팩토리의 @Component 어노테이션에 지정된 문자열과 일치해야 합니다.
```
<ipojo xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="org.apache.felix.ipojo http://felix.apache.org/ipojo/schemas/CURRENT/core.xsd"
xmlns="org.apache.felix.ipojo">
	<instance component="random-logger-factory" />
</ipojo>
```

## 빌드
메이븐 및 JDK 6 이상의 버전이 설치되어 있다면, 프로젝트 최상위 디렉터리에서 아래와 같이 명령을 실행합니다.
```
$ mvn clean install
[INFO] Scanning for projects...
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO]
[INFO] Logpresso Logger SDK
[INFO] Logpresso Logger Example
```

## 설치 및 테스트
텔넷 (7004번 포트) 혹은 SSH (7022번 포트)를 통해 로그프레소 쉘에 접속 후, 아래와 같이 확장 번들을 설치합니다:
```
araqne> bundle.install file:///logpresso-sdk-logger/logpresso-logger-example/target/logpresso-logger-example-1.0.0.jar
bundle [109] loaded

araqne> bundle.start 109
bundle 109 started
```
로그프레소나 센트리 버전의 차이, 혹은 추가 번들 설치 상태에 따라 새로 설치되는 번들의 ID는 다를 수 있습니다. 아래와 같이 새로 설치한 수집기를 확인할 수 있습니다:
```
araqne> logapi.loggerFactories random
Logger Factories (filtered)
---------------------
+--------------+--------------+
| factory name | display name |
+--------------+--------------+
| local\random | Random       |
+--------------+--------------+
```

이제 로그프레소 수집기 설정 화면에서, 새로운 Random 수집기를 확인할 수 있습니다. 수집주기를 1초로 설정하면 매 초마다 random number: 문자열로 시작하는 로그가 수집됩니다.

## 번들 업데이트 및 삭제
코드를 수정한 후 다시 배포하려면 아래와 같이 입력합니다:
```
araqne> bundle.update 109
bundle 109 updated, old timestamp: 2016-11-23 00:44:24+0900, new timestamp: 2016-11-23 01:27:40+0900

araqne> bundle.refresh
bundles are refreshed.
```
설치한 번들을 삭제하려면 아래와 같이 입력합니다:
```
araqne> bundle.uninstall 109
```
기타 사용 가능한 쉘 명령을 보려면 TAB 키 자동완성 및 매개변수 없는 명령어 실행 시 출력되는 도움말을 확인하시기 바랍니다.