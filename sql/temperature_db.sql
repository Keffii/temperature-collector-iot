drop database temperatureDB;
create database temperatureDB;
use temperatureDB;

create table temperature (
    id int not null auto_increment primary key,
    temperature double not null,
    created_at datetime default current_timestamp,
    update_at datetime default current_timestamp on update current_timestamp
);
