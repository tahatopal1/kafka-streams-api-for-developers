package com.learnkafkastreams.service;

import com.learnkafkastreams.domain.OrderType;
import com.learnkafkastreams.domain.OrdersCountPerStoreByWindows;
import com.learnkafkastreams.domain.OrdersRevenuePerStoreByWindows;
import com.learnkafkastreams.domain.TotalRevenue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.learnkafkastreams.service.OrderService.mapOrderType;
import static com.learnkafkastreams.topology.OrdersTopology.*;

@Service
@Slf4j
public class OrdersWindowService {

    private OrderStoreService orderStoreService;

    public OrdersWindowService(OrderStoreService orderStoreService) {
        this.orderStoreService = orderStoreService;
    }

    public List<OrdersCountPerStoreByWindows> getAllOrdersCountWindowsByType(String orderType) {

        var countWindowsStore = getCountWindowsStore(orderType);

        var orderTypeEnum = mapOrderType(orderType);

        var countWindowsIterator = countWindowsStore.all();
        var spliterator = Spliterators.spliteratorUnknownSize(countWindowsIterator, 0);
        return StreamSupport.stream(spliterator, false)
                .map(windowedLongKeyValue -> {
                    printLocalDateTimes(windowedLongKeyValue.key, windowedLongKeyValue.value);
                    return new OrdersCountPerStoreByWindows(
                            windowedLongKeyValue.key.key(),
                            windowedLongKeyValue.value,
                            orderTypeEnum,
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().startTime(),
                                    ZoneId.of("GMT")),
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().endTime(),
                                    ZoneId.of("GMT"))

                    );
                })
                .toList();

    }

    public ReadOnlyWindowStore<String, Long> getCountWindowsStore(String orderType) {

        return switch (orderType) {
            case GENERAL_ORDERS -> orderStoreService.ordersWindowCountStore(GENERAL_ORDERS_COUNT_WINDOWS);
            case RESTAURANT_ORDERS -> orderStoreService.ordersWindowCountStore(RESTAURANT_ORDERS_COUNT_WINDOWS);
            default -> throw new IllegalStateException("Not a Valid Option");
        };
    }

    public List<OrdersCountPerStoreByWindows> getAllOrdersCountByWindows() {

        var generalOrdersCountByWindows = getAllOrdersCountWindowsByType(GENERAL_ORDERS);

        var restaurantOrdersCountByWindows = getAllOrdersCountWindowsByType(RESTAURANT_ORDERS);

        return Stream.of(generalOrdersCountByWindows, restaurantOrdersCountByWindows)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<OrdersCountPerStoreByWindows> getAllOrdersCountByWindows(LocalDateTime fromTime, LocalDateTime toTime) {

        var fromTimeInstant = fromTime.toInstant(ZoneOffset.UTC);
        var toTimeInstant = toTime.toInstant(ZoneOffset.UTC);

        log.info("fromTimeInstant : {} , toTimeInstant : {} ", fromTimeInstant, toTimeInstant);

        var generalOrdersCountByWindows = getCountWindowsStore(GENERAL_ORDERS)
                .fetchAll(fromTimeInstant, toTimeInstant)
                //.backwardAll() //This is to send the results in the reverse order
                ;

        var generalAllOrderCountPerStoreByWindows = mapToOrderCountPerStoreByWindows(generalOrdersCountByWindows, OrderType.GENERAL);

        var restaurantOrdersCountByWindows = getCountWindowsStore(RESTAURANT_ORDERS)
                .fetchAll(fromTimeInstant, toTimeInstant);

        var restaurantOrderCountPerStoreByWindows = mapToOrderCountPerStoreByWindows(restaurantOrdersCountByWindows, OrderType.RESTAURANT);

        return Stream.of(generalAllOrderCountPerStoreByWindows, restaurantOrderCountPerStoreByWindows)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());


    }


    private static List<OrdersCountPerStoreByWindows> mapToOrderCountPerStoreByWindows(KeyValueIterator<Windowed<String>, Long> ordersCountByWindows, OrderType orderType) {
        var spliterator = Spliterators.spliteratorUnknownSize(ordersCountByWindows, 0);
        return StreamSupport.stream(spliterator, false)
                .map(windowedLongKeyValue -> {
                    printLocalDateTimes(windowedLongKeyValue.key, windowedLongKeyValue.value);
                    return new OrdersCountPerStoreByWindows(
                            windowedLongKeyValue.key.key(),
                            windowedLongKeyValue.value,
                            orderType,
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().startTime(),
                                    ZoneId.of("GMT")),
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().endTime(),
                                    ZoneId.of("GMT"))

                    );
                })
                .toList();
    }


    public List<OrdersRevenuePerStoreByWindows> getAllOrdersRevenueWindowsByType(String orderType) {

        var revenueWindowsStore = getRevenueWindowsStore(orderType);

        var orderTypeEnum = mapOrderType(orderType);

        var revenueWindowsIterator = revenueWindowsStore.all();
        var spliterator = Spliterators.spliteratorUnknownSize(revenueWindowsIterator, 0);
        return StreamSupport.stream(spliterator, false)
                .map(windowedLongKeyValue -> {
                    printLocalDateTimes(windowedLongKeyValue.key, windowedLongKeyValue.value);
                    return new OrdersRevenuePerStoreByWindows(
                            windowedLongKeyValue.key.key(),
                            windowedLongKeyValue.value,
                            orderTypeEnum,
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().startTime(),
                                    ZoneId.of("GMT")),
                            LocalDateTime.ofInstant(windowedLongKeyValue.key.window().endTime(),
                                    ZoneId.of("GMT"))

                    );
                })
                .toList();
    }

    public ReadOnlyWindowStore<String, TotalRevenue> getRevenueWindowsStore(String orderType) {

        return switch (orderType) {
            case GENERAL_ORDERS -> orderStoreService.ordersWindowRevenueStore(GENERAL_ORDERS_REVENUE_WINDOWS);
            case RESTAURANT_ORDERS -> orderStoreService.ordersWindowRevenueStore(RESTAURANT_ORDERS_REVENUE_WINDOWS);
            default -> throw new IllegalStateException("Not a Valid Option");
        };
    }
}
