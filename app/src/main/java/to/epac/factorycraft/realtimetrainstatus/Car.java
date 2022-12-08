package to.epac.factorycraft.realtimetrainstatus;

public class Car {

    public int carLoad;
    public int passengerCount;
    public String carName;
    public int passengerLoad;

    public Car(int carLoad, int passengerCount, String carName, int passengerLoad) {
        this.carLoad = carLoad;
        this.passengerCount = passengerCount;
        this.carName = carName;
        this.passengerLoad = passengerLoad;
    }
}
