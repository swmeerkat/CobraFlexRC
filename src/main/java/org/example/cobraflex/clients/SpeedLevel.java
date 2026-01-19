package org.example.cobraflex.clients;

public enum SpeedLevel {
  LEVEL_ONE,
  LEVEL_TWO,
  LEVEL_THREE,
  LEVEL_FOUR;

  public double getSpeed() {
    switch (this) {
      case LEVEL_ONE -> {
        return 200;
      }
      case LEVEL_TWO -> {
        return 600;
      }
      case LEVEL_THREE -> {
        return 1200;
      }
      case LEVEL_FOUR -> {
        return 1800;
      }
    }
    return 0;
  }

}
