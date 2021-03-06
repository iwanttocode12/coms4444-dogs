package dogs.g4;

import java.util.*;

import dogs.sim.*;
import dogs.sim.Dictionary;


public class Player extends dogs.sim.Player {

    final String firstSignal = "zythum";
    final String finalPosition = "ready";

    private List<Owner> coOwners = new ArrayList<>();
    private List<Owner> g1Owners = new ArrayList<>();
    private List<Owner> g2Owners = new ArrayList<>();
    private List<Owner> g3Owners = new ArrayList<>();
    private List<Owner> g5Owners = new ArrayList<>();
    private List<Owner.OwnerName> coOwnersNames = new ArrayList<>();
    private List<Owner.OwnerName> g1OwnersNames = new ArrayList<>();
    private List<Owner.OwnerName> g2OwnersNames = new ArrayList<>();
    private List<Owner.OwnerName> g3OwnersNames = new ArrayList<>();
    private List<Owner.OwnerName> g5OwnersNames = new ArrayList<>();
    private ParkLocation lastThrow = new ParkLocation();
    private ParkLocation lastPosition = new ParkLocation();
    private Map<Owner, OwnerDistance> allLocationMap = new HashMap<>();
    private Double dividedAngle = 0.0;
    private boolean weInPosition = false;
    private boolean coopInPosition = false;

    /**
     * Player constructor
     *
     * @param rounds           number of rounds
     * @param numDogsPerOwner  number of dogs per owner
     * @param numOwners	       number of owners
     * @param seed             random seed
     * @param simPrinter       simulation printer
     *
     */
    public Player(Integer rounds, Integer numDogsPerOwner, Integer numOwners, Integer seed, Random random, SimPrinter simPrinter) {
        super(rounds, numDogsPerOwner, numOwners, seed, random, simPrinter);
    }

    /**
     * Choose command/directive for next round
     *
     * @param round        current round
     * @param myOwner      my owner
     * @param otherOwners  all other owners in the park
     * @return             a directive for the owner's next move
     *
     */
    public Directive chooseDirective(Integer round, Owner myOwner, List<Owner> otherOwners) {
        Directive myDirective = new Directive();

        if (round == 1) {
            myDirective.signalWord = firstSignal;
            myDirective.instruction = Directive.Instruction.CALL_SIGNAL;
            return myDirective;
        }

        if (round == 6) {
            for (Owner owner : otherOwners) {
                if (owner.getCurrentSignal().equals(firstSignal)) {
                    this.coOwners.add(owner);
                }
                if (owner.getCurrentSignal().equals("papaya")) {
                    this.g1Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals("two")) {
                    this.g2Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals("three")) {
                    this.g3Owners.add(owner);
                }
                if (owner.getCurrentSignal().equals("Zyzzogeton")) {
                    this.g5Owners.add(owner);
                }
            }
            this.coOwnersNames = getNames(this.coOwners);
            this.g1OwnersNames = getNames(this.g1Owners);
            this.g2OwnersNames = getNames(this.g2Owners);
            this.g3OwnersNames = getNames(this.g3Owners);
            this.g5OwnersNames = getNames(this.g5Owners);
        }

        updateOwnersList(otherOwners);

        if (this.coOwners.isEmpty() && this.g1Owners.size() > 1) {
            myDirective.instruction = Directive.Instruction.MOVE;
            this.lastPosition = new ParkLocation(this.g1Owners.get(0).getLocation().getRow() + 10, this.g1Owners.get(0).getLocation().getColumn());
            if (!myOwner.getLocationAsString().equals(this.lastPosition.toString())) {
                myDirective.parkLocation = this.lastPosition;
                return myDirective;
            }
            else {
                List<String> g1Signals = getOtherOwnersSignals(this.g1Owners);
                if (g1Signals.contains("here")) this.coopInPosition = true;
                if (this.coopInPosition && round % 100 == 1) {
                    myDirective.dogToPlayWith = getMyAvailableDog(myOwner);
                    myDirective.instruction = Directive.Instruction.THROW_BALL;
                    this.lastThrow = this.g1Owners.get(0).getLocation();
                    myDirective.parkLocation = this.lastThrow;
                    return myDirective;
                }
            }
        }
        else {
            this.allLocationMap = getCircularLocations(200, 200, myOwner, 39.0, false, 10.0);
            ParkLocation finalLocation = this.allLocationMap.get(myOwner).location;
            updateDistances(otherOwners, myOwner);

            if (finalLocation.getRow().intValue() != myOwner.getLocation().getRow().intValue() ||
                    finalLocation.getColumn().intValue() != myOwner.getLocation().getColumn().intValue()) {
                myDirective.instruction = Directive.Instruction.MOVE;
                this.lastPosition = getMyCircularNextLocation(myOwner, finalLocation);
                myDirective.parkLocation = this.lastPosition;
                return myDirective;
            }

            List<Dog> waitingDogs = getWaitingDogs(myOwner, otherOwners);

            Comparator<Dog> bySpeed = (Dog d1, Dog d2) -> Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed());
            Comparator<Dog> byWaitingTime = (Dog d1, Dog d2) -> Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining());
            Collections.sort(waitingDogs, byWaitingTime);

            int numRoundsPositioning = getNumRoundsPositioning(this.allLocationMap);

            if (round > numRoundsPositioning && round < numRoundsPositioning + 10) {
                myDirective.signalWord = finalPosition;
                myDirective.instruction = Directive.Instruction.CALL_SIGNAL;
                return myDirective;
            }
            else if (round > numRoundsPositioning) {
                if (waitingDogs.size() > 0) {
                    int i = 0;
                    while (waitingDogs.get(i).getExerciseTimeRemaining() == 0.0 && myOwner.hasDog(waitingDogs.get(i))) {
                        if (i < waitingDogs.size()) i++;
                    }
                    myDirective.dogToPlayWith = waitingDogs.get(i);

                    myDirective.instruction = Directive.Instruction.THROW_BALL;
//            simPrinter.println(ownersDistances.get(0).location.toString().equals(this.lastThrow.toString()));
//            simPrinter.println(ownersDistances.get(0).location.toString());
//            simPrinter.println(this.lastThrow.toString());
                    this.lastThrow = getClockWiseNextLane(this.allLocationMap, myOwner, waitingDogs.get(i).getBreed());
                    myDirective.parkLocation = this.lastThrow;

                    if (this.g1Owners.get(0).hasDog(waitingDogs.get(i)) && round % 4 == 1 && myOwner.getNameAsString().equals("Carol")) {
                        this.lastThrow = this.g1Owners.get(0).getLocation();
                        myDirective.parkLocation = this.lastThrow;
                    }

                }
            }

//        simPrinter.println("----------------------------");
//        for (Owner owner : this.allLocationMap.keySet()) {
//            simPrinter.println(allLocationMap.get(owner).dist);
//        }
//        simPrinter.println("----------------------------");
        }

        return myDirective;
    }

    private void updateOwnersList(List<Owner> owners) {
        this.coOwners.clear();
        this.g1Owners.clear();
        this.g2Owners.clear();
        this.g3Owners.clear();
        this.g5Owners.clear();

        for (Owner owner : owners) {
            if (this.coOwnersNames.contains(owner.getNameAsEnum())) this.coOwners.add(owner);
            else if (this.g1OwnersNames.contains(owner.getNameAsEnum())) this.g1Owners.add(owner);
            else if (this.g2OwnersNames.contains(owner.getNameAsEnum())) this.g2Owners.add(owner);
            else if (this.g3OwnersNames.contains(owner.getNameAsEnum())) this.g3Owners.add(owner);
            else if (this.g5OwnersNames.contains(owner.getNameAsEnum())) this.g5Owners.add(owner);
        }

    }

    private Dog getMyAvailableDog(Owner owner) {
        for (Dog dog : owner.getDogs()) {
            if (dog.isWaitingForItsOwner()) return dog;
        }
        return owner.getDogs().get(0);
    }

    private ParkLocation getClockWiseNextLane(Map<Owner, OwnerDistance> owners, Owner myOwner, DogReference.Breed breed) {
        ParkLocation target = getClockWiseNext(this.allLocationMap, myOwner);

        Double beta = 0.0;
        Double breedAngle = (180 - Math.toDegrees(Math.atan((beta/2)/39))*2);
        Double alpha = breedAngle/2;
        int xIndic = 0;
        int yIndic = 0;

        if (myOwner.getLocation().getRow() > target.getRow()) xIndic = 1;
        else xIndic = -1;
        if (myOwner.getLocation().getColumn() > target.getColumn()) yIndic = 1;
        else yIndic = -1;

//        if (myOwner.getLocation().getRow() > target.getRow() && myOwner.getLocation().getColumn() > target.getColumn()) {
//            xIndic = 1;
//            yIndic = -1;
//        }
//        else if (myOwner.getLocation().getRow() > target.getRow() && myOwner.getLocation().getColumn() < target.getColumn()) {
//            xIndic = 1;
//            yIndic = 1;
//        }
//        else if (myOwner.getLocation().getRow() < target.getRow() && myOwner.getLocation().getColumn() > target.getColumn()) {
//            xIndic = -1;
//            yIndic = 1;
//        }
//        else {
//            xIndic = -1;
//            yIndic = -1;
//        }


        switch (breed) {
            case LABRADOR:
                return new ParkLocation(target.getRow(), target.getColumn());
            case POODLE:
                return new ParkLocation(target.getRow()+xIndic*beta/3*Math.sin(alpha), target.getColumn()+yIndic*beta/3*Math.cos(alpha));
            case SPANIEL:
                return new ParkLocation(target.getRow()+xIndic*beta/3*2*Math.sin(alpha), target.getColumn()+yIndic*beta/3*2*Math.cos(alpha));
            case TERRIER:
                return new ParkLocation(target.getRow()+xIndic*beta*Math.sin(alpha), target.getColumn()+yIndic*beta*Math.cos(alpha));
        }
        return target;
    }

    private ParkLocation getClockWiseNext(Map<Owner, OwnerDistance> owners, Owner myOwner) {

        Double myAngle = owners.get(myOwner).angle;

        for (OwnerDistance owner : owners.values()) {
//            simPrinter.println("--------------------------");
//            simPrinter.println(owner.angle);
//            simPrinter.println(myAngle);
//            simPrinter.println(myAngle + this.dividedAngle);
//            simPrinter.println("--------------------------");
            if (Math.round(myAngle + this.dividedAngle) == 360.0) {
                if (owner.angle == 0.0) return owner.location;
            }
            else if (Math.round(owner.angle) == Math.round(myAngle + this.dividedAngle)) {
                return owner.location;
            }
        }

        return new ParkLocation(0.0,0.0);
    }

    private Owner getOwnerWithName(Owner.OwnerName name) {
        for (Owner owner : this.allLocationMap.keySet()) {
            if (owner.getNameAsEnum() == name) return owner;
        }
        return new Owner();
    }

    private Integer getNumRoundsPositioning(Map<Owner, OwnerDistance> finalPositions) {
        Owner farOwner = new Owner();
        Double farOwnerDist = 0.0;
        int numRounds = 0;

        for (Owner owner : finalPositions.keySet()) {
            if (getDist(finalPositions.get(owner).location, new ParkLocation(0.0,0.0)) > farOwnerDist) {
                farOwnerDist = getDist(finalPositions.get(owner).location, new ParkLocation(0.0,0.0));
                farOwner = owner;
            }
        }

        numRounds = (int) (finalPositions.get(farOwner).location.getRow() / 5) + 1 +
                (int) (finalPositions.get(farOwner).location.getColumn() / 5) + 1;

        return numRounds * 5;
    }

    private List<Owner.OwnerName> getNames(List <Owner> owners) {
        List<Owner.OwnerName> names = new ArrayList<>();

        for (Owner owner : owners) {
            names.add(owner.getNameAsEnum());
        }

        return names;
    }

    private void updateDistances(List<Owner> otherOwners, Owner myOwner) {
        for (Owner owner : otherOwners) {
            if (this.coOwnersNames.contains(owner.getNameAsEnum())) {
                Owner o = getOwnerWithName(owner.getNameAsEnum());
                this.allLocationMap.get(o).dist = getDist(owner.getLocation(), myOwner.getLocation());
            }
        }
    }

    private Double getDist(ParkLocation l1, ParkLocation l2) {
        Double x1 = l1.getRow();
        Double y1 = l1.getColumn();
        Double x2 = l2.getRow();
        Double y2 = l2.getColumn();

        return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
    }

    private ParkLocation getMyCircularNextLocation(Owner myOwner, ParkLocation finalLocation) {
        Double x = myOwner.getLocation().getRow();
        Double y = myOwner.getLocation().getColumn();

//        simPrinter.println(myOwner.getNameAsString());
//        simPrinter.println(myOwner.getLocationAsString());
//        simPrinter.println(finalLocation.toString());

        if (finalLocation.getRow().intValue() != x.intValue()) {
            if (finalLocation.getRow() >= x + 5) {
                x = x + 5;
            }
            else if (finalLocation.getRow() < x + 5) {
                x = x + Math.abs(finalLocation.getRow() - x);
            }
        }
        else if (finalLocation.getColumn().intValue() != y.intValue()) {
            if (finalLocation.getColumn() >= y + 5) {
                y = y + 5;
            }
            else if (finalLocation.getColumn() < y + 5) {
                y = y + Math.abs(finalLocation.getColumn() - y);
            }
        }

        return new ParkLocation(x, y);
    }

    private Map<Owner, OwnerDistance> getCircularLocations(Integer ySize, Integer xSize, Owner myOwner, Double maxDist, boolean centralized, Double margin) {
        Map<Owner, OwnerDistance> circularLocations = new HashMap<>();
        List<Owner> owners = new ArrayList<>(this.coOwners);
        owners.add(myOwner);

        Comparator<Owner> byName = (Owner o1, Owner o2) -> o1.getNameAsString().compareTo(o2.getNameAsString());
        Collections.sort(owners, byName);

        this.dividedAngle = 360.0/(owners.size());
        Double angle = 0.0;
        Double maxRadius = 0.0;
        Double radius = (maxDist/2)/Math.sin(Math.toRadians(this.dividedAngle/2));
        if (centralized) maxRadius = Math.min(ySize, xSize)/2.0;
        else maxRadius = radius + margin;

        for (Owner owner : owners) {
            Double x = maxRadius - radius*Math.cos(Math.toRadians(angle));
            Double y = maxRadius - radius*Math.sin(Math.toRadians(angle));
            circularLocations.put(owner, new OwnerDistance(owner, 0.0, new ParkLocation(x,y), angle));
            angle = angle + this.dividedAngle;
        }

        return circularLocations;
    }

    private List<String> getOtherOwnersSignals(List<Owner> otherOwners) {
        List<String> otherOwnersSignals = new ArrayList<>();
        for (Owner otherOwner : otherOwners)
            if (!otherOwner.getCurrentSignal().equals("_"))
                otherOwnersSignals.add(otherOwner.getCurrentSignal());
        return otherOwnersSignals;
    }

    private List<Dog> getWaitingDogs(Owner myOwner, List<Owner> otherOwners) {
        List<Dog> waitingDogs = new ArrayList<>();
        for (Owner otherOwner : otherOwners) {
            for (Dog dog : otherOwner.getDogs()) {
                if (dog.isWaitingForOwner(myOwner))
                    waitingDogs.add(dog);
            }
        }

        Comparator<Dog> bySpeed = (Dog d1, Dog d2) -> Double.compare(d1.getRunningSpeed(), d2.getRunningSpeed());
        Collections.sort(waitingDogs, bySpeed.reversed());
        Comparator<Dog> byWaitingTime = (Dog d1, Dog d2) -> Double.compare(d1.getWaitingTimeRemaining(), d2.getWaitingTimeRemaining());
        Collections.sort(waitingDogs, byWaitingTime.reversed());

        for (Dog dog : myOwner.getDogs()) {
            if (dog.isWaitingForItsOwner())
                waitingDogs.add(dog);
        }

        Collections.reverse(waitingDogs);

        return waitingDogs;
    }

    private class OwnerDistance implements Comparable<OwnerDistance> {
        Owner owner;
        Double dist;
        ParkLocation location;
        Double angle;

        OwnerDistance(Owner owner, Double dist, ParkLocation location, Double angle) {
            this.owner = owner;
            this.dist = dist;
            this.location = location;
            this.angle = angle;
        }

        @Override
        public int compareTo(OwnerDistance other) {
            return Double.compare(this.dist, other.dist);
        }

        public String toString() {
            return this.owner.getNameAsString() + ": " + this.dist + ": " + this.location.toString();
        }

    }

}

