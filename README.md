StaticSimSpeedChanger
=====================
Normalises unit stats to a different sim speed, creating a server mod intended for use on local servers (where we can change the sim speed). The idea is to have the sim run slower than normal, but for the game to appear to run at normal speed.

Usage: StaticSimSpeedChanger [ops] [path_to_pa_dir] [speedup_factor]

  [ops]
    --help : This help

  path_to_pa_dir : The path to the PA directory, containing the PA executable
                   and the media directory

  speedup_factor : The number of times you have to call /api/sim_slower to
                   have a normal speed game, default 3

The formula to determine sim speed is 10*(0.8)^speedup_factor. For example the
default of speedup_factor = 3 yields 10*(0.8)^3 = 5.12 FPS