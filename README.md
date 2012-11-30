# About

This project reresents a proof-of-concept of a way I want to explore for possible redesigning Loaders
into pieces that individually know how to

* Build a java.sql.Statement/CallableStatement
* Execute such statements
* Process the java.sql.ResultSet objects that result from executing such statements

This is still an incubating proposal.  See the proposal at <https://github.com/hibernate/hibernate-orm/wiki/Proposal---Loader-redesign>

It's Jira is https://hibernate.onjira.com/browse/HHH-7841