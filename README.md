*Сборка(В случае запуска JMH после каждого запуска)*
1. Создать бд parser с пользователем и паролем parser
2. mvn clean -U && cd algorithm-db && mvn package
3. cd .. && mvn -T 4 package -DskipTests

*Запуск*
1. Положить архивы в папку archives рядом с jar
2. Запустить тесты

   
*3 состояния компании:*
1. Транзакция выполнена
   1. Текущий документ неосновной - пропуск
   2. Текущий документ основной - становимся в очередь на обновление компании, пока ждем работаем по другим документам
2. Поток выполняет действия по основной и/или связанной компаниям:
   1. Становимся в очередь на выполнение по основной и/или связанным компаниям, пока ждем работаем по другим документам
   2. Если до нас дошла очередь:
      1. Транзакция выполнена успешно: очищаем очередь, пропускаем действия, либо повторяем действия для связанных несохраненных компаний
      2. Транзакция не выполнена успешно: переходим к шагу 3
3. Ни один поток не выполняет действия по данной компании: 
   1. Блокируем основную компанию, блокируем несохраненные ранее связанные компании
   2. Выполняем действия:
      1. Успех: 
         1. Коммитим транзакцию
         2. Сохраняем в список успешных транзакций
         3. Убираем блок на компании
      2. Неуспех
         1. Откатываем транзакцию
         2. Убираем блок на компании

*NATIVE IMAGE*
1. mvn spring-boot:build-image
2. docker run --rm -p 8080:8080 algorithm-impl-1.0-SNAPSHOT

*JVM*

Для native image:
````
-H:±UseCompressedReferences
````

Для JVM:
````
-Xmx 256M -Xms 256M -XX:+UseZGC
````