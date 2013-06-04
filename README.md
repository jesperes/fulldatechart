FullDateChart
=============

Small Java program which creates a .ics file with all your missing dates for a 
full date chart.

Usage
=====

java -jar fulldatechart.jar /path/to/myfinds-pq.gpx youruserid

The first parameter is your "My finds" pocket-query. The second one is your 
numeric user id. You can find this inside the gpx file.

The calendar file is written to the current directory as "fulldatechart.ics".
You can then import it into your calendar application. Each day you need to
log a cache to fill an empty slot in your date chart will have a full day event
called "Cache day".

