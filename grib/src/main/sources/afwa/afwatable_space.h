
/*
 * parameter table for the Air Force Weather Agency
 * center = 57 subcenter = 3
 */

struct ParmTable parm_table_afwa_space[256] = {
   /* 0 */ {"var0", "undefined"},
   /* 1 */ {"PRES", "Pressure [Pa]"},
   /* 2 */ {"PRMSL", "Pressure reduced to MSL [Pa]"},
   /* 3 */ {"PTEND", "Pressure tendency [Pa/s]"},
   /* 4 */ {"PVORT", "Potential Vorticity [K m^2/kg s]"},
   /* 5 */ {"ICAHT", "ICAO Standard Atmosphere Reference Height [m]"},
   /* 6 */ {"GP", "Geopotential [m^2/s^2]"},
   /* 7 */ {"HGT", "Geopotential height [gpm]"},
   /* 8 */ {"DIST", "Geometric height [m]"},
   /* 9 */ {"HSTDV", "Std dev of height [m]"},
   /* 10 */ {"TOZNE", "Total ozone [Dobson]"},
   /* 11 */ {"TMP", "Temp. [K]"},
   /* 12 */ {"VTMP", "Virtual temp. [K]"},
   /* 13 */ {"POT", "Potential temp. [K]"},
   /* 14 */ {"EPOT", "Pseudo-adiabatic pot. temp. [K]"},
   /* 15 */ {"TMAX", "Max. temp. [K]"},
   /* 16 */ {"TMIN", "Min. temp. [K]"},
   /* 17 */ {"DPT", "Dew point temp. [K]"},
   /* 18 */ {"DEPR", "Dew point depression [K]"},
   /* 19 */ {"LAPR", "Lapse rate [K/m]"},
   /* 20 */ {"VISIB", "Visibility [m]"},
   /* 21 */ {"RDSP1", "Radar spectra (1) [non-dim]"},
   /* 22 */ {"RDSP2", "Radar spectra (2) [non-dim]"},
   /* 23 */ {"RDSP3", "Radar spectra (3) [non-dim]"},
   /* 24 */ {"PLI", "Parcel lifted index (to 500 hPa [K]"},
   /* 25 */ {"TMPA", "Temp. anomaly [K]"},
   /* 26 */ {"PRESA", "Pressure anomaly [Pa]"},
   /* 27 */ {"GPA", "Geopotential height anomaly [gpm]"},
   /* 28 */ {"WVSP1", "Wave spectra (1) [non-dim]"},
   /* 29 */ {"WVSP2", "Wave spectra (2) [non-dim]"},
   /* 30 */ {"WVSP3", "Wave spectra (3) [non-dim]"},
   /* 31 */ {"WDIR", "Wind direction [deg]"},
   /* 32 */ {"WIND", "Wind speed [m/s]"},
   /* 33 */ {"UGRD", "u wind [m/s]"},
   /* 34 */ {"VGRD", "v wind [m/s]"},
   /* 35 */ {"STRM", "Stream function [m^2/s]"},
   /* 36 */ {"VPOT", "Velocity potential [m^2/s]"},
   /* 37 */ {"MNTSF", "Montgomery stream function [m^2/s^2]"},
   /* 38 */ {"SGCVV", "Sigma coord. vertical velocity [/s]"},
   /* 39 */ {"VVEL", "Pressure vertical velocity [Pa/s]"},
   /* 40 */ {"DZDT", "Geometric vertical velocity [m/s]"},
   /* 41 */ {"ABSV", "Absolute vorticity [/s]"},
   /* 42 */ {"ABSD", "Absolute divergence [/s]"},
   /* 43 */ {"RELV", "Relative vorticity [/s]"},
   /* 44 */ {"RELD", "Relative divergence [/s]"},
   /* 45 */ {"VUCSH", "Vertical u shear [/s]"},
   /* 46 */ {"VVCSH", "Vertical v shear [/s]"},
   /* 47 */ {"DIRC", "Direction of current [deg]"},
   /* 48 */ {"SPC", "Speed of current [m/s]"},
   /* 49 */ {"UOGRD", "u of current [m/s]"},
   /* 50 */ {"VOGRD", "v of current [m/s]"},
   /* 51 */ {"SPFH", "Specific humidity [kg/kg]"},
   /* 52 */ {"RH", "Relative humidity [%]"},
   /* 53 */ {"MIXR", "Humidity mixing ratio [kg/kg]"},
   /* 54 */ {"PWAT", "Precipitable water [kg/m^2]"},
   /* 55 */ {"VAPP", "Vapor pressure [Pa]"},
   /* 56 */ {"SATD", "Saturation deficit [Pa]"},
   /* 57 */ {"EVP", "Evaporation [kg/m^2]"},
   /* 58 */ {"CICE", "Cloud Ice [kg/m^2]"},
   /* 59 */ {"PRATE", "Precipitation rate [kg/m^2/s]"},
   /* 60 */ {"TSTM", "Thunderstorm probability [%]"},
   /* 61 */ {"APCP", "Total precipitation [kg/m^2]"},
   /* 62 */ {"NCPCP", "Large scale precipitation [kg/m^2]"},
   /* 63 */ {"ACPCP", "Convective precipitation [kg/m^2]"},
   /* 64 */ {"SRWEQ", "Snowfall rate water equiv. [kg/m^2/s]"},
   /* 65 */ {"WEASD", "Accum. snow [kg/m^2]"},
   /* 66 */ {"SNOD", "Snow depth [m]"},
   /* 67 */ {"MIXHT", "Mixed layer depth [m]"},
   /* 68 */ {"TTHDP", "Transient thermocline depth [m]"},
   /* 69 */ {"MTHD", "Main thermocline depth [m]"},
   /* 70 */ {"MTHA", "Main thermocline anomaly [m]"},
   /* 71 */ {"TCDC", "Total cloud cover [%]"},
   /* 72 */ {"CDCON", "Convective cloud cover [%]"},
   /* 73 */ {"LCDC", "Low level cloud cover [%]"},
   /* 74 */ {"MCDC", "Mid level cloud cover [%]"},
   /* 75 */ {"HCDC", "High level cloud cover [%]"},
   /* 76 */ {"CWAT", "Cloud water [kg/m^2]"},
   /* 77 */ {"BLI", "Best lifted index to 500 hPa [K]"},
   /* 78 */ {"SNOC", "Convective snow [kg/m^2]"},
   /* 79 */ {"SNOL", "Large scale snow [kg/m^2]"},
   /* 80 */ {"WTMP", "Water temp. [K]"},
   /* 81 */ {"LAND", "Land-sea mask [1=land; 0=sea]"},
   /* 82 */ {"DSLM", "Deviation of sea level from mean [m]"},
   /* 83 */ {"SFCR", "Surface roughness [m]"},
   /* 84 */ {"ALBDO", "Albedo [%]"},
   /* 85 */ {"TSOIL", "Soil temp. [K]"},
   /* 86 */ {"SOILM", "Soil moisture content [kg/m^2]"},
   /* 87 */ {"VEG", "Vegetation [%]"},
   /* 88 */ {"SALTY", "Salinity [kg/kg]"},
   /* 89 */ {"DEN", "Density [kg/m^3]"},
   /* 90 */ {"RUNOF", "Runoff [kg/m^2]"},
   /* 91 */ {"ICEC", "Ice concentration [ice=1;no ice=0]"},
   /* 92 */ {"ICETK", "Ice thickness [m]"},
   /* 93 */ {"DICED", "Direction of ice drift [deg]"},
   /* 94 */ {"SICED", "Speed of ice drift [m/s]"},
   /* 95 */ {"UICE", "u of ice drift [m/s]"},
   /* 96 */ {"VICE", "v of ice drift [m/s]"},
   /* 97 */ {"ICEG", "Ice growth rate [m/s]"},
   /* 98 */ {"ICED", "Ice divergence [/s]"},
   /* 99 */ {"SNOM", "Snow melt [kg/m^2]"},
   /* 100 */ {"HTSGW", "Sig height of wind waves and swell [m]"},
   /* 101 */ {"WVDIR", "Direction of wind waves [deg]"},
   /* 102 */ {"WVHGT", "Sig height of wind waves [m]"},
   /* 103 */ {"WVPER", "Mean period of wind waves [s]"},
   /* 104 */ {"SWDIR", "Direction of swell waves [deg]"},
   /* 105 */ {"SWELL", "Sig height of swell waves [m]"},
   /* 106 */ {"SWPER", "Mean period of swell waves [s]"},
   /* 107 */ {"DIRPW", "Primary wave direction [deg]"},
   /* 108 */ {"PERPW", "Primary wave mean period [s]"},
   /* 109 */ {"DIRSW", "Secondary wave direction [deg]"},
   /* 110 */ {"PERSW", "Secondary wave mean period [s]"},
   /* 111 */ {"NSWRS", "Net short wave (surface) [W/m^2]"},
   /* 112 */ {"NLWRS", "Net long wave (surface) [W/m^2]"},
   /* 113 */ {"NSWRT", "Net short wave (top) [W/m^2]"},
   /* 114 */ {"NLWRT", "Net long wave (top) [W/m^2]"},
   /* 115 */ {"LWAVR", "Long wave [W/m^2]"},
   /* 116 */ {"SWAVR", "Short wave [W/m^2]"},
   /* 117 */ {"GRAD", "Global radiation [W/m^2]"},
   /* 118 */ {"BRTMP", "Brightness temperature [K]"},
   /* 119 */ {"LWRAD", "Radiance (with respect to wave number) [W/m sr]"},
   /* 120 */ {"SWRAD", "Radiance (with respect to wave length) [W/m^3 sr]"},
   /* 121 */ {"LHTFL", "Latent heat flux [W/m^2]"},
   /* 122 */ {"SHTFL", "Sensible heat flux [W/m^2]"},
   /* 123 */ {"BLYDP", "Boundary layer dissipation [W/m^2]"},
   /* 124 */ {"UFLX", "Zonal momentum flux [N/m^2]"},
   /* 125 */ {"VFLX", "Meridional momentum flux [N/m^2]"},
   /* 126 */ {"WMIXE", "Wind mixing energy [J]"},
   /* 127 */ {"IMGD", "Image data [integer]"},
   /* 128 */ {" EDEN", " Electron Density [cm-3]"},
   /* 129 */ {" OPLUSDEN", " Oxygen Ion Density [cm-3] "},
   /* 130 */ {" HPLUSDEN", " Hydrogen Density [cm-3] "},
   /* 131 */ {" N2O2DEN", " Sum of Molecular Nitrogen and Oxygen Density [cm-3] "},
   /* 132 */ {" ITEMP", " Ion Temperature [K] "},
   /* 133 */ {" ETEMP", " Electron Temperature [K] "},
   /* 134 */ {" FOF2", " F2 Layer Critical Frequency [MHz] "},
   /* 135 */ {" HMF2", " F2 Layer Height [Km] "},
   /* 136 */ {" FOF1", " F1 Layer Critical Frequency [MHz] "},
   /* 137 */ {" HMF1", " F1 Layer Height [Km] "},
   /* 138 */ {" FOE", " E Layer Critical Frequency [MHz] "},
   /* 139 */ {" HME", " E Layer Height [Km] "},
   /* 140 */ {" TEC", " Total Electron Content [TECU] "},
   /* 141 */ {" MLT", " Magnetic Local Time [hours] "},
   /* 142 */ {" GMLAT", " Geomagnetic Latitude [Deg] "},
   /* 143 */ {" GMLON", " Geomagnetic Longitude [Deg] "},
   /* 144 */ {" ALTIONO", " Altitude   [Km] "},
   /* 145 */ {" var145", " blank   [n/a] "},
   /* 146 */ {" var146", " blank [n/a] "},
   /* 147 */ {" var147", " blank [n/a] "},
   /* 148 */ {" var148", " blank [n/a] "},
   /* 149 */ {" var149", " blank [n/a] "},
   /* 150 */ {" var150", " blank [n/a] "},
   /* 151 */ {" var151", " blank [n/a] "},
   /* 152 */ {" var152", " blank [n/a] "},
   /* 153 */ {" var153", " blank [n/a] "},
   /* 154 */ {" var154", " blank [n/a] "},
   /* 155 */ {" var155", " blank   [n/a] "},
   /* 156 */ {" var156", " blank  [n/a] "},
   /* 157 */ {" var157", " blank   [n/a] "},
   /* 158 */ {" var158", " blank   [n/a] "},
   /* 159 */ {" var159", " blank   [n/a] "},
   /* 160 */ {" var160", " blank   [n/a] "},
   /* 161 */ {" var161", " blank   [n/a] "},
   /* 162 */ {" var162", " blank  [n/a] "},
   /* 163 */ {" var163", " blank   [n/a] "},
   /* 164 */ {" var164", " blank   [n/a] "},
   /* 165 */ {" var165", " blank [n/a] "},
   /* 166 */ {" var166", " blank [n/a] "},
   /* 167 */ {" var167", " blank [n/a] "},
   /* 168 */ {" var168", " blank [n/a] "},
   /* 169 */ {" var169", " blank [n/a] "},
   /* 170 */ {" var170", " blank [n/a] "},
   /* 171 */ {" var171", " blank [n/a] "},
   /* 172 */ {" var172", " blank [n/a] "},
   /* 173 */ {" var173", " blank [n/a] "},
   /* 174 */ {" var174", " blank   [n/a] "},
   /* 175 */ {" var175", " blank   [n/a] "},
   /* 176 */ {" var176", " blank   [n/a] "},
   /* 177 */ {" var177", " blank   [n/a] "},
   /* 178 */ {" var178", " blank   [n/a] "},
   /* 179 */ {" var179", " blank   [n/a] "},
   /* 180 */ {" var180", " blank   [n/a] "},
   /* 181 */ {" var181", " blank   [n/a] "},
   /* 182 */ {" var182", " blank   [n/a] "},
   /* 183 */ {" var183", " blank   [n/a] "},
   /* 184 */ {" var184", " blank   [n/a] "},
   /* 185 */ {" var185", " blank  [n/a] "},
   /* 186 */ {" var186", " blank  [n/a] "},
   /* 187 */ {" var187", " blank  [n/a] "},
   /* 188 */ {" var188", " blank  [n/a] "},
   /* 189 */ {" var189", " blank  [n/a] "},
   /* 190 */ {" var190", " blank  [n/a] "},
   /* 191 */ {" var191", " blank  [n/a] "},
   /* 192 */ {" var192", " blank  [n/a] "},
   /* 193 */ {" var193", " blank  [n/a] "},
   /* 194 */ {" var194", " blank  [n/a] "},
   /* 195 */ {" var195", " blank  [n/a] "},
   /* 196 */ {" var196", " blank   [n/a] "},
   /* 197 */ {" var197", " blank   [n/a] "},
   /* 198 */ {" var198", " blank   [n/a] "},
   /* 199 */ {" var199", " blank   [n/a] "},
   /* 200 */ {" Var200", " blank   [n/a] "},
   /* 201 */ {" var201", " blank   [n/a] "},
   /* 202 */ {" var202", " blank   [n/a] "},
   /* 203 */ {" var203", " blank   [n/a] "},
   /* 204 */ {" var204", " blank   [n/a] "},
   /* 205 */ {" var205", " blank   [n/a] "},
   /* 206 */ {" var206", " blank   [n/a] "},
   /* 207 */ {" var207", " blank   [n/a] "},
   /* 208 */ {" var208", " blank   [n/a] "},
   /* 209 */ {" var209", " blank   [n/a] "},
   /* 210 */ {" var210", " blank   [n/a] "},
   /* 211 */ {" var211", " blank   [n/a] "},
   /* 212 */ {" var212", " blank   [n/a] "},
   /* 213 */ {" var213", " blank   [n/a] "},
   /* 214 */ {" var214", " blank [n/a]"},
   /* 215 */ {" var215", " blank [n/a]"},
   /* 216 */ {" var216", " blank [n/a]"},
   /* 217 */ {" var217", " blank [n/a]"},
   /* 218 */ {" var218", " blank [n/a]"},
   /* 219 */ {" var219", " blank [n/a]"},
   /* 220 */ {" var220", " blank [n/a]"},
   /* 221 */ {" var221", " blank [n/a]"},
   /* 222 */ {" var222", " blank [n/a]"},
   /* 223 */ {" var223", " blank [n/a]"},
   /* 224 */ {" var224", " blank [n/a]"},
   /* 225 */ {" var225", " blank [n/a]"},
   /* 226 */ {" var226", " blank [n/a]"},
   /* 227 */ {" var227", " blank [n/a]"},
   /* 228 */ {" var228", " blank [n/a]"},
   /* 229 */ {" var229", " blank [n/a]"},
   /* 230 */ {" var230", " blank [n/a]"},
   /* 231 */ {" var231", " blank [n/a]"},
   /* 232 */ {" var232", " blank [n/a]"},
   /* 233 */ {" var233", " blank [n/a]"},
   /* 234 */ {" var234", " blank   [n/a] "},
   /* 235 */ {" var235", " blank   [n/a] "},
   /* 236 */ {" var236", " blank [n/a]"},
   /* 237 */ {" var237", " blank [n/a]"},
   /* 238 */ {" var238", " blank [n/a]"},
   /* 239 */ {" var239", " blank [n/a]"},
   /* 240 */ {" var240", " blank [n/a]"},
   /* 241 */ {" var241", " blank [n/a]"},
   /* 242 */ {" var242", " blank [n/a]"},
   /* 243 */ {" var243", " blank [n/a]"},
   /* 244 */ {" var244", " blank [n/a]"},
   /* 245 */ {" var245", " blank [n/a]"},
   /* 246 */ {" var246", " blank [n/a]"},
   /* 247 */ {" var247", " blank [n/a]"},
   /* 248 */ {" var248", " blank [n/a]"},
   /* 249 */ {" var249", " blank [n/a]"},
   /* 250 */ {" var250", " blank [n/a]"},
   /* 251 */ {" var251", " blank [n/a]"},
   /* 252 */ {" var252", " blank [n/a]"},
   /* 253 */ {" var253", " blank [n/a]"},
   /* 254 */ {" var254", " blank [n/a]"},
   /* 255 */ {" Missing "},

};


