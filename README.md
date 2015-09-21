# SMRemapper
A tool that reads .smmap's to deobfuscate and reobfuscate StarMade

## Command Line
Usage: java -jar SMRemapper.jar {input} {output} {mapping} {libs folder} {reverse (true/false)} {keep source (true/false)}

Example: java -jar SMRemapper.jar StarMade.jar StarMade-Deobf.jar ???_raw_min.smtmap libs false true

## API
SMRemapper(ILog log) - Creates a new SMRemapper instance

reset() - Resets the remapper to defaults

resetMappings() - Resets any mappings loaded by loadMapping(...)

resetClasses() - Resets all class data caused by laodLib(...) or remap(...)

loadMapping(File mapping, boolean reverse) - Loads the mappings, it can also reverse them.

displayMappingInfo(File mapping) - Outputs the mapping information

loadLib(File path) - Loads a library

remap(File input, File output) - Remaps the input to the output

setKeepSource(boolean keepSource) - Sets if the source information should be kept in the classes (line numbers etc)
