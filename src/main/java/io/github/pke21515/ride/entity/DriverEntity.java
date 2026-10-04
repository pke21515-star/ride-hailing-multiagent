package io.github.pke21515.ride.entity;

import jakarta.persistence.*;

/** 司机：首次启动种子数据落库，之后状态持久化。 */
@Entity
@Table(name = "drivers")
public class DriverEntity {

    @Id
    @Column(length = 16)
    public String id;

    @Column(nullable = false, length = 32)
    public String name;

    public Double lat, lng;
    public Double rating;
    public Double acceptRate;
    public Boolean idle = true;
    public Integer todayOrders = 0;
}
