package org.example.cobraflex.clients;

public enum SpeedLevel {
  LEVEL_ONE,
  LEVEL_TWO,
  LEVEL_THREE,
  LEVEL_FOUR;

  public int getSpeed() {
    switch (this) {
      case LEVEL_ONE -> {
        return 300;
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
    return LEVEL_ONE.getSpeed();
  }

}
