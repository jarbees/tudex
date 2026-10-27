package tuda.campus.mobile

data class MapAnnotationData(
    val annotationTitle: String,
    val buildingName: String,
    val latitude: Double,
    val longitude: Double,
    val clusteringID: String?
)

val mapAnnotations: List<MapAnnotationData> = listOf(
    // Stadtmitte
    // Stadtmitte 1
    MapAnnotationData("S1|01", latitude = 49.87501, longitude = 8.65654, clusteringID = "Stadtmitte", buildingName = "Universitätszentrum, karo 5, Audimax"),
    MapAnnotationData("S1|02", latitude = 49.87622, longitude = 8.65653, clusteringID = "Stadtmitte", buildingName = "Altes Hauptgebäude (Westflügel)"),
    MapAnnotationData("S1|03", latitude = 49.87698, longitude = 8.65651, clusteringID = "Stadtmitte", buildingName = "Altes Hauptgebäude"),
    MapAnnotationData("S1|05", latitude = 49.87719, longitude = 8.65758, clusteringID = "Stadtmitte", buildingName = "Maschinenhaus"),
    MapAnnotationData("S1|08", latitude = 49.87624, longitude = 8.65870, clusteringID = "Stadtmitte", buildingName = "Maschinenbau"),
    MapAnnotationData("S1|09", latitude = 49.87616, longitude = 8.65826, clusteringID = "Stadtmitte", buildingName = "Maschinenbauhalle Mitte"),
    MapAnnotationData("S1|10", latitude = 49.87569, longitude = 8.65871, clusteringID = "Stadtmitte", buildingName = "Druckmaschinen"),
    MapAnnotationData("S1|11", latitude = 49.87569, longitude = 8.65784, clusteringID = "Stadtmitte", buildingName = "Mensa mit Otto-Berndt-Halle"),
    MapAnnotationData("S1|13", latitude = 49.87515, longitude = 8.65813, clusteringID = "Stadtmitte", buildingName = "Pädagogik / Berufspädagogik"),
    MapAnnotationData("S1|14", latitude = 49.87528, longitude = 8.65873, clusteringID = "Stadtmitte", buildingName = "Papierfabrikation"),
    MapAnnotationData("S1|15", latitude = 49.87555, longitude = 8.65953, clusteringID = "Stadtmitte", buildingName = "Psychologie"),
    MapAnnotationData("S1|17", latitude = 49.87723, longitude = 8.65809, clusteringID = "Stadtmitte", buildingName = "Sportwissenschaft (Hofreite Vorderhaus)"),
    MapAnnotationData("S1|18", latitude = 49.87735, longitude = 8.65830, clusteringID = "Stadtmitte", buildingName = "Sportwissenschaft (Hofreite Hinterhaus)"),
    MapAnnotationData("S1|19", latitude = 49.87566, longitude = 8.65913, clusteringID = "Stadtmitte", buildingName = "Verfügungsgebäude (Container)"),
    MapAnnotationData("S1|20", latitude = 49.87654, longitude = 8.65768, clusteringID = "Stadtmitte", buildingName = "Universitäts- und Landesbibliothek, ULB"),
    MapAnnotationData("S1|21", latitude = 49.87720, longitude = 8.65844, clusteringID = "Stadtmitte", buildingName = "Kinderhaus II – Stadtmitte"),
    MapAnnotationData("S1|22", latitude = 49.87513, longitude = 8.65750, clusteringID = "Stadtmitte", buildingName = "Karl-Plagge-Haus"),
    MapAnnotationData("S1|60", latitude = 49.87699, longitude = 8.65840, clusteringID = "Stadtmitte", buildingName = "Verwaltung"),
    
    // Stadtmitte 2
    MapAnnotationData("S2|01", latitude = 49.87743, longitude = 8.65514, clusteringID = "Stadtmitte", buildingName = "Rau-Bau"),
    MapAnnotationData("S2|02", latitude = 49.87750, longitude = 8.65451, clusteringID = "Stadtmitte", buildingName = "Robert-Piloty-Gebäude"),
    MapAnnotationData("S2|03", latitude = 49.87817, longitude = 8.65406, clusteringID = "Stadtmitte", buildingName = "Studierendenhaus"),
    MapAnnotationData("S2|04", latitude = 49.87822, longitude = 8.65482, clusteringID = "Stadtmitte", buildingName = "Festkörperphysik"),
    MapAnnotationData("S2|05", latitude = 49.87797, longitude = 8.65501, clusteringID = "Stadtmitte", buildingName = "Physik Werkstätten"),
    MapAnnotationData("S2|06", latitude = 49.87783, longitude = 8.65524, clusteringID = "Stadtmitte", buildingName = "Physik Hörsäle"),
    MapAnnotationData("S2|07", latitude = 49.87754, longitude = 8.65561, clusteringID = "Stadtmitte", buildingName = "Physik Institute"),
    MapAnnotationData("S2|08", latitude = 49.87753, longitude = 8.65606, clusteringID = "Stadtmitte", buildingName = "Physik Hörsaal Uhrturm"),
    MapAnnotationData("S2|09", latitude = 49.87792, longitude = 8.65574, clusteringID = "Stadtmitte", buildingName = "Physik Lichttechnik"),
    MapAnnotationData("S2|10", latitude = 49.87779, longitude = 8.65635, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S2|11", latitude = 49.87830, longitude = 8.65604, clusteringID = "Stadtmitte", buildingName = "Physik Institutsgebäude"),
    MapAnnotationData("S2|12", latitude = 49.87853, longitude = 8.65586, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S2|13", latitude = 49.87850, longitude = 8.65546, clusteringID = "Stadtmitte", buildingName = "Fraunhofer-Institut für Betriebsfestigkeit und Systemzuverlässigkeit LBF, Bereich Kunststoffe"),
    MapAnnotationData("S2|14", latitude = 49.87869, longitude = 8.65639, clusteringID = "Stadtmitte", buildingName = "Kernphysik / S-DALINAC"),
    MapAnnotationData("S2|15", latitude = 49.87890, longitude = 8.65719, clusteringID = "Stadtmitte", buildingName = "Mathematik / Physik"),
    MapAnnotationData("S2|17", latitude = 49.87934, longitude = 8.65439, clusteringID = "Stadtmitte", buildingName = "Halbleitertechnik"),
    MapAnnotationData("S2|20", latitude = 49.87811, longitude = 8.65699, clusteringID = "Stadtmitte", buildingName = "Zentrum für IT-Sicherheit (CYSEC)"),
    MapAnnotationData("S2|50", latitude = 49.87774, longitude = 8.65463, clusteringID = "Stadtmitte", buildingName = "Physikalisches Grundpraktikum"),
    
    // Stadtmitte 3
    MapAnnotationData("S3|05", latitude = 49.87425, longitude = 8.65992, clusteringID = "Stadtmitte", buildingName = "Fraunhofer-Institut für Graphische Datenverarbeitung (IGD)"),
    MapAnnotationData("S3|06", latitude = 49.87349, longitude = 8.66067, clusteringID = "Stadtmitte", buildingName = "Elektrotechnik und Informationstechnik (Hans-Busch-Institut)"),
    MapAnnotationData("S3|07", latitude = 49.87371, longitude = 8.66001, clusteringID = "Stadtmitte", buildingName = "Ernst-Neufert-Halle"),
    MapAnnotationData("S3|09", latitude = 49.87376, longitude = 8.65826, clusteringID = "Stadtmitte", buildingName = "Elektrotechnik und Informationstechnik, Hallen mit Werkstätten"),
    MapAnnotationData("S3|10", latitude = 49.87370, longitude = 8.65766, clusteringID = "Stadtmitte", buildingName = "Elektrotechnik und Informationstechnik, Institute"),
    MapAnnotationData("S3|11", latitude = 49.87356, longitude = 8.65719, clusteringID = "Stadtmitte", buildingName = "Elektrotechnik und Informationstechnik, Hörsäle"),
    MapAnnotationData("S3|12", latitude = 49.87316, longitude = 8.65517, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss De la Fosse-Bau, Präsidium der TU Darmstadt, Universitäts- und Landesbibliothek (ULB), Sparkassen-Saal"),
    MapAnnotationData("S3|13", latitude = 49.87349, longitude = 8.65481, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss Kaisersaalbau, Hörsaal"),
    MapAnnotationData("S3|14", latitude = 49.87377, longitude = 8.65528, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss Kirchenbau, Orgelsaal"),
    MapAnnotationData("S3|15", latitude = 49.87352, longitude = 8.65577, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss Glockenbau, Schlossmuseum"),
    MapAnnotationData("S3|16", latitude = 49.87419, longitude = 8.65557, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss Wallhaus"),
    MapAnnotationData("S3|17", latitude = 49.87419, longitude = 8.65514, clusteringID = "Stadtmitte", buildingName = "Wissenschaftsschloss Brückenhaus"),
    MapAnnotationData("S3|19", latitude = 49.87426, longitude = 8.66068, clusteringID = "Stadtmitte", buildingName = "Instituts- und Verwaltungsgebäude"),
    MapAnnotationData("S3|20", latitude = 49.87470, longitude = 8.66063, clusteringID = "Stadtmitte", buildingName = "Instituts- und Verwaltungsgebäude"),
    MapAnnotationData("S3|21", latitude = 49.87406, longitude = 8.65881, clusteringID = "Stadtmitte", buildingName = "Verfügungsgebäude Elektrotechnik"),
    
    // Stadtmitte 4
    MapAnnotationData("S4|01", latitude = 49.87525, longitude = 8.65533, clusteringID = "Stadtmitte", buildingName = "Mollerbau (Haus der Geschichte / Staatsarchiv)"),
    MapAnnotationData("S4|02", latitude = 49.87471, longitude = 8.64899, clusteringID = "Stadtmitte", buildingName = "Werkstoffkunde/ Staatliche Materialprüfungsanstalt (MPA)"),
    MapAnnotationData("S4|07", latitude = 49.87385, longitude = 8.63163, clusteringID = "Stadtmitte", buildingName = "Studienkolleg für ausländische Studierende"),
    MapAnnotationData("S4|10", latitude = 49.87760, longitude = 8.64012, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S4|13", latitude = 49.87356, longitude = 8.64838, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S4|14", latitude = 49.87244, longitude = 8.63535, clusteringID = "Stadtmitte", buildingName = "FB Informatik, HDT, emergenCity, CCHH, DLR"),
    MapAnnotationData("S4|15", latitude = 49.86288, longitude = 8.64617, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S4|22", latitude = 49.87809, longitude = 8.64224, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S4|23", latitude = 49.87761, longitude = 8.64212, clusteringID = "Stadtmitte", buildingName = "Institutsgebäude"),
    MapAnnotationData("S4|26", latitude = 49.87815, longitude = 8.63846, clusteringID = "Stadtmitte", buildingName = "Verwaltungsgebäude"),
    MapAnnotationData("S4|28", latitude = 49.81566, longitude = 8.61907, clusteringID = "Stadtmitte", buildingName = "Bürogebäude"),
    MapAnnotationData("S4|29", latitude = 49.81566, longitude = 8.61907, clusteringID = "Stadtmitte", buildingName = "Versuchshalle"),
    MapAnnotationData("S4|84", latitude = 49.88345, longitude = 8.68504, clusteringID = "Stadtmitte", buildingName = "Georg-Christoph-Lichtenberg-Haus, Internationales Begegnungszentrum (IBZ)"),
    MapAnnotationData("S4|85", latitude = 49.88264, longitude = 8.68484, clusteringID = "Stadtmitte", buildingName = "Reithalle mit Stallungen"),
    
    // Lichtwiese
    // Lichtwiese 1
    MapAnnotationData("L1|01", latitude = 49.86133, longitude = 8.68273, clusteringID = "Lichtwiese", buildingName = "Maschinenbau Institute"),
    MapAnnotationData("L1|02", latitude = 49.86152, longitude = 8.68368, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 1, U-Station Maschinenbau"),
    MapAnnotationData("L1|03", latitude = 49.86153, longitude = 8.68482, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 5"),
    MapAnnotationData("L1|04", latitude = 49.86153, longitude = 8.68521, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 4 (Prüfstand)"),
    MapAnnotationData("L1|05", latitude = 49.86081, longitude = 8.68522, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 3"),
    MapAnnotationData("L1|06", latitude = 49.86091, longitude = 8.68390, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 2"),
    MapAnnotationData("L1|07", latitude = 49.86227, longitude = 8.68371, clusteringID = "Lichtwiese", buildingName = "Maschinenbauhalle 6"),
    MapAnnotationData("L1|09", latitude = 49.86090, longitude = 8.68478, clusteringID = "Lichtwiese", buildingName = "CO2-Versuchshalle"),
    MapAnnotationData("L1|10", latitude = 49.86190, longitude = 8.68435, clusteringID = "Lichtwiese", buildingName = "Gerhard-Pahl-Zentrum (Lehrzentrum Maschinenbau)"),
    MapAnnotationData("L1|12", latitude = 49.86112, longitude = 8.68437, clusteringID = "Lichtwiese", buildingName = "Fahrzeughalle"),
    MapAnnotationData("L1|13", latitude = 49.86191, longitude = 8.68327, clusteringID = "Lichtwiese", buildingName = "Maschinenakustikhalle SAM"),
    MapAnnotationData("L1|14", latitude = 49.86224, longitude = 8.68252, clusteringID = "Lichtwiese", buildingName = "FlowFactory"),
    MapAnnotationData("L1|16", latitude = 49.86025, longitude = 8.68323, clusteringID = "Lichtwiese", buildingName = "CRA-Gebäude"),
    MapAnnotationData("L1|17", latitude = 49.86020, longitude = 8.68246, clusteringID = "Lichtwiese", buildingName = "AMC-Gebäude"),
    
    // Lichtwiese 2
    MapAnnotationData("L2|01", latitude = 49.86027, longitude = 8.68150, clusteringID = "Lichtwiese", buildingName = "Materialwissenschaft"),
    MapAnnotationData("L2|02", latitude = 49.86026, longitude = 8.68032, clusteringID = "Lichtwiese", buildingName = "Organische Chemie, Poststelle Lichtwiese"),
    MapAnnotationData("L2|03", latitude = 49.86058, longitude = 8.67990, clusteringID = "Lichtwiese", buildingName = "Chemiehörsaalgebäude"),
    MapAnnotationData("L2|04", latitude = 49.86025, longitude = 8.67920, clusteringID = "Lichtwiese", buildingName = "Physikalische Chemie"),
    MapAnnotationData("L2|05", latitude = 49.85993, longitude = 8.67857, clusteringID = "Lichtwiese", buildingName = "Anorganische Chemie, Merck-TU-Juniorlabor"),
    MapAnnotationData("L2|06", latitude = 49.86049, longitude = 8.67855, clusteringID = "Lichtwiese", buildingName = "Center of Smart Interfaces (CSI)"),
    MapAnnotationData("L2|07", latitude = 49.85920, longitude = 8.67931, clusteringID = "Lichtwiese", buildingName = "M³ Laborgebäude"),
    MapAnnotationData("L2|08", latitude = 49.85976, longitude = 8.67788, clusteringID = "Lichtwiese", buildingName = "Lehrlaborgebäude (LLG)"),
    MapAnnotationData("L2|50", latitude = 49.86025, longitude = 8.67920, clusteringID = "Lichtwiese", buildingName = "Hausverwaltung Chemie"),
    MapAnnotationData("L2|51", latitude = 49.86025, longitude = 8.67920, clusteringID = "Lichtwiese", buildingName = "Lernzentrum Chemie"),
    MapAnnotationData("L2|66", latitude = 49.85927, longitude = 8.67869, clusteringID = "Lichtwiese", buildingName = "Entsorgungszentrum"),
    
    // Lichtwiese 3
    MapAnnotationData("L3|01", latitude = 49.86118, longitude = 8.67663, clusteringID = "Lichtwiese", buildingName = "Architektur"),
    MapAnnotationData("L3|02", latitude = 49.86054, longitude = 8.67713, clusteringID = "Lichtwiese", buildingName = "Solar Decathlon 2007"),
    MapAnnotationData("L3|03", latitude = 49.86053, longitude = 8.67676, clusteringID = "Lichtwiese", buildingName = "Solar Decathlon 2009"),
    MapAnnotationData("L3|60", latitude = 49.86021, longitude = 8.67720, clusteringID = "Lichtwiese", buildingName = "UniKITA, Krabbelgruppen"),
    MapAnnotationData("L3|61", latitude = 49.86011, longitude = 8.67651, clusteringID = "Lichtwiese", buildingName = "UniKITA, Waldkindergarten"),
    MapAnnotationData("L3|62", latitude = 49.86011, longitude = 8.67651, clusteringID = "Lichtwiese", buildingName = "Kinderhaus I – Lichtwiese"),
    
    // Lichtwiese 4
    MapAnnotationData("L4|01", latitude = 49.86159, longitude = 8.67961, clusteringID = "Lichtwiese", buildingName = "Mensa Lichtwiese, Lieferanschrift"),
    MapAnnotationData("L4|02", latitude = 49.86124, longitude = 8.68124, clusteringID = "Lichtwiese", buildingName = "Hörsaal- und Medienzentrum (HMZ)"),
    
    // Lichtwiese 5
    MapAnnotationData("L5|01", latitude = 49.86213, longitude = 8.67854, clusteringID = "Lichtwiese", buildingName = "Bauingenieurwesen, Institutsgebäude 1"),
    MapAnnotationData("L5|02", latitude = 49.86281, longitude = 8.67854, clusteringID = "Lichtwiese", buildingName = "Bauingenieurhalle 1"),
    MapAnnotationData("L5|03", latitude = 49.86267, longitude = 8.67812, clusteringID = "Lichtwiese", buildingName = "Bauingenieurhalle 4"),
    MapAnnotationData("L5|04", latitude = 49.86294, longitude = 8.67812, clusteringID = "Lichtwiese", buildingName = "Bauingenieurhalle 3"),
    MapAnnotationData("L5|05", latitude = 49.86320, longitude = 8.67811, clusteringID = "Lichtwiese", buildingName = "Bauingenieurhalle 2"),
    MapAnnotationData("L5|06", latitude = 49.86228, longitude = 8.68001, clusteringID = "Lichtwiese", buildingName = "Bauingenieurwesen Institutsgebäude 2"),
    MapAnnotationData("L5|07", latitude = 49.86241, longitude = 8.67926, clusteringID = "Lichtwiese", buildingName = "Bauingenieurhalle 5"),
    MapAnnotationData("L5|08", latitude = 49.86318, longitude = 8.67936, clusteringID = "Lichtwiese", buildingName = "Hochleistungsrechner"),
    MapAnnotationData("L5|09", latitude = 49.86282, longitude = 8.67934, clusteringID = "Lichtwiese", buildingName = "Glas-Competence-Center (GCC), Bauingenieurwesen"),
    MapAnnotationData("L5|60", latitude = 49.86322, longitude = 8.67992, clusteringID = "Lichtwiese", buildingName = "Wertstoffzentrum"),
    
    // Lichtwiese 6
    MapAnnotationData("L6|01", latitude = 49.86260, longitude = 8.68170, clusteringID = "Lichtwiese", buildingName = "Institutsgebäude (SI-Gebäude)"),
    MapAnnotationData("L6|02", latitude = 49.86311, longitude = 8.68167, clusteringID = "Lichtwiese", buildingName = "Institutsgebäude (SIE-Gebäude)"),
    MapAnnotationData("L6|03", latitude = 49.86369, longitude = 8.68102, clusteringID = "Lichtwiese", buildingName = "ETA-Fabrik"),
    
    // Botanischer Garten
    // Botanischer Garten 1 (West)
    MapAnnotationData("B1|01", latitude = 49.86938, longitude = 8.68018, clusteringID = "Bot. Garten", buildingName = "Botanik, Zoologie (Institutsgebäude mit Energiehaus)"),
    MapAnnotationData("B1|02", latitude = 49.86973, longitude = 8.68032, clusteringID = "Bot. Garten", buildingName = "Botanik (Altbau)"),
    MapAnnotationData("B1|03", latitude = 49.87003, longitude = 8.67977, clusteringID = "Bot. Garten", buildingName = "Großes Gewächshaus"),
    MapAnnotationData("B1|04", latitude = 49.87003, longitude = 8.67977, clusteringID = "Bot. Garten", buildingName = "Versuchsgewächshaus"),
    // B1|05 ???
    MapAnnotationData("B1|06", latitude = 49.87003, longitude = 8.67977, clusteringID = "Bot. Garten", buildingName = "Kalthauspflanzen (Gewächshaus)"),
    MapAnnotationData("B1|07", latitude = 49.87003, longitude = 8.67977, clusteringID = "Bot. Garten", buildingName = "Orchideenhaus (Gewächshaus)"),
    MapAnnotationData("B1|08", latitude = 49.86902, longitude = 8.68012, clusteringID = "Bot. Garten", buildingName = "Mobi-Office I"),
    MapAnnotationData("B1|70", latitude = 49.86965, longitude = 8.67986, clusteringID = "Bot. Garten", buildingName = "Infopavillon Botanischer Garten"),
    
    // Botanischer Garten 2 (Ost)
    MapAnnotationData("B2|01", latitude = 49.86915, longitude = 8.68112, clusteringID = "Bot. Garten", buildingName = "Angewandte Geowissenschaften und Hörsaal"),
    MapAnnotationData("B2|02", latitude = 49.86904, longitude = 8.68157, clusteringID = "Bot. Garten", buildingName = "Angewandte Geowissenschaften"),
    MapAnnotationData("B2|03", latitude = 49.86848, longitude = 8.68149, clusteringID = "Bot. Garten", buildingName = "Biologie Verfügungsgebäude"),
    MapAnnotationData("B2|04", latitude = 49.86809, longitude = 8.68135, clusteringID = "Bot. Garten", buildingName = "Ökologisches Versuchsgewächshaus"),
    MapAnnotationData("B2|05", latitude = 49.86879, longitude = 8.68208, clusteringID = "Bot. Garten", buildingName = "Bioinnovationszentrum I (BIZ I)"),
    MapAnnotationData("B2|06", latitude = 49.86910, longitude = 8.68217, clusteringID = "Bot. Garten", buildingName = "Bioinnovationszentrum II (BIZ II)"),
    MapAnnotationData("B2|07", latitude = 49.86892, longitude = 8.68260, clusteringID = "Bot. Garten", buildingName = "Mobi-Office II"),
    MapAnnotationData("B2|08", latitude = 49.86867, longitude = 8.68249, clusteringID = "Bot. Garten", buildingName = "MerckLifeLab (MLL)"),
    MapAnnotationData("B2|61", latitude = 49.86891, longitude = 8.68087, clusteringID = "Bot. Garten", buildingName = "Studierendenzentrum Botanik, Cafe Bios"),
    
    // Hochschulstadion
    MapAnnotationData("H1|01", latitude = 49.85854, longitude = 8.67046, clusteringID = "Hochschulstadion", buildingName = "Sporthallen"),
    MapAnnotationData("H1|02", latitude = 49.85992, longitude = 8.67087, clusteringID = "Hochschulstadion", buildingName = "Hochschulstadion, Laufbahn und Sitzterrassen"),
    MapAnnotationData("H1|03", latitude = 49.86101, longitude = 8.67385, clusteringID = "Hochschulstadion", buildingName = "unifit"),
    MapAnnotationData("H1|04", latitude = 49.86036, longitude = 8.67182, clusteringID = "Hochschulstadion", buildingName = "Hochschulstadion Kinderbadebecken"),
    MapAnnotationData("H1|05", latitude = 49.86014, longitude = 8.67176, clusteringID = "Hochschulstadion", buildingName = "Hochschulstadion Garderoben"),
    MapAnnotationData("H1|06", latitude = 49.85976, longitude = 8.67163, clusteringID = "Hochschulstadion", buildingName = "Hochschulstadion Schwimmbad mit Tribüne"),
    MapAnnotationData("H1|07", latitude = 49.86101, longitude = 8.67426, clusteringID = "Hochschulstadion", buildingName = "Kletterhalle Alpenverein"),
    MapAnnotationData("H1|08", latitude = 49.86111, longitude = 8.67088, clusteringID = "Hochschulstadion", buildingName = "Bürogebäude Unisport-Zentrum"),
    MapAnnotationData("H1|82", latitude = 49.86100, longitude = 8.66997, clusteringID = "Hochschulstadion", buildingName = "Otto-Wolfskehl-Haus (Internationales Gästehaus)"),
    MapAnnotationData("H1|83", latitude = 49.86077, longitude = 8.66877, clusteringID = "Hochschulstadion", buildingName = "Studierendenwohnheim"),
    MapAnnotationData("H1|84", latitude = 49.86080, longitude = 8.67240, clusteringID = "Hochschulstadion", buildingName = "uniKITA – Waldkindergarten"),
    
    // Windkanal
    // Windkanal 1 (Windkanal)
    MapAnnotationData("W1|01", latitude = 49.85910, longitude = 8.58930, clusteringID = "Windkanal", buildingName = "Flugtechnik (Labore)"),
    MapAnnotationData("W1|03", latitude = 49.85933, longitude = 8.58922, clusteringID = "Windkanal", buildingName = "Flugtechnik (Werkstätten)"),
    MapAnnotationData("W1|04", latitude = 49.85936, longitude = 8.58016, clusteringID = "Windkanal", buildingName = "Flugtechnik (Windkanal und Halle)"),
    MapAnnotationData("W1|05", latitude = 49.85919, longitude = 8.58981, clusteringID = "Windkanal", buildingName = "Minimaxoffice"),
    MapAnnotationData("W1|06", latitude = 49.85878, longitude = 8.58992, clusteringID = "Windkanal", buildingName = "Prüfhalle Hochspannungstrasse (GIL-Halle)"),
    MapAnnotationData("W1|07", latitude = 49.85995, longitude = 8.59040, clusteringID = "Windkanal", buildingName = "Bürogebäude"),
    MapAnnotationData("W1|08", latitude = 49.85964, longitude = 8.589012, clusteringID = "Windkanal", buildingName = "Halle"),
    
    // Windkanal 2 (Flugplatz)
    MapAnnotationData("W2|01", latitude = 49.85650, longitude = 8.59285, clusteringID = "Windkanal", buildingName = "Tower (August-Euler-Informationszentrum)"),
    MapAnnotationData("W2|02", latitude = 49.85654, longitude = 8.59510, clusteringID = "Windkanal", buildingName = "Halle 1"),
    MapAnnotationData("W2|03", latitude = 49.85659, longitude = 8.59318, clusteringID = "Windkanal", buildingName = "Halle 2"),
    MapAnnotationData("W2|04", latitude = 49.85662, longitude = 8.59336, clusteringID = "Windkanal", buildingName = "Halle 3"),
    MapAnnotationData("W2|05", latitude = 49.85652, longitude = 8.59190, clusteringID = "Windkanal", buildingName = "Douglas DC-8 (August-Euler-Informationszentrum)"),

)