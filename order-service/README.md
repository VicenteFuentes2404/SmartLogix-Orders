# Orders MS

Microservicio de pedidos, items multiples, strategy por rol y saga de compra.

## Ejecutar

```powershell
mvn spring-boot:run
```

Puerto: `8082`

## Pruebas y cobertura

```powershell
mvn test
mvn jacoco:report
mvn clean verify
```

Reporte JaCoCo:

```text
target/site/jacoco/index.html
```

Cobertura verificada: `62,34%`.

## Swagger

```text
http://localhost:8082/swagger-ui.html
http://localhost:8082/v3/api-docs
```

## DTOs principales

- `OrderRequestDTO`
- `OrderItemRequestDTO`
- `OrderResponseDTO`
- `OrderItemResponseDTO`
- `CustomerSnapshotDTO`
- `ShippingAddressDTO`
- `OrderDeliveredEvent`

La saga coordina inventory, payments y shipments con mocks en pruebas unitarias.
