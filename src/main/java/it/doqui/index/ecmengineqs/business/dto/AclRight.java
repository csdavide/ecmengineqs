package it.doqui.index.ecmengineqs.business.dto;

import lombok.Getter;

public class AclRight {
    public static final int NONE     = 0b000000;
    public static final int READ     = 0b000001;
    public static final int UPDATE   = 0b000010;
    public static final int CREATE   = 0b000100;
    public static final int CHECKOUT = 0b001000;
    public static final int DELETE   = 0b010000;
    public static final int ADMIN    = 0b111111;

    @Getter
    private int value = NONE;

    public static boolean hasRight(AclRight x, int right) {
        return x != null && (x.value & right) == right;
    }

    public static AclRight addPermission(AclRight x, AclPermission p) {
        if (x == null) {
            x = new AclRight();
        }

        switch (p) {
            case Read:
            case Consumer:
                x.value |=  READ;
                break;
            case Editor:
                x.value |=  READ | UPDATE | CHECKOUT;
                break;
            case Contributor:
                x.value |=  READ | CREATE;
                break;
            case Collaborator:
                x.value |=  READ | CREATE | UPDATE | CHECKOUT;
                break;
            case Coordinator:
                x.value |=  READ | CREATE | UPDATE | CHECKOUT | DELETE;
                break;
            case RepoAdmin:
                x.value |= ADMIN;
                break;
            default:
                break;
        }

        return x;
    }

    @Override
    public String toString() {
        return "AclRight{" +
            "value=" + Integer.toBinaryString(value) +
            '}';
    }
}
