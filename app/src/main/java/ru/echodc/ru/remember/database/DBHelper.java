package ru.echodc.ru.remember.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import ru.echodc.ru.remember.model.ModelTask;

public class DBHelper extends SQLiteOpenHelper {

    //    Версия базы
//    public static final int DATABASE_VERSION = 1;

    public static final int DATABASE_VERSION = 2;

    //    Имя базы данных
    public static final String DATABASE_NAME = "reminder_db";

    //    Имя таблицы в базе
    public static final String TASK_TABLE = "task_table";

    //    Колонки в таблице
    public static final String TASK_TITLE_COLUMN = "task_title";
    public static final String TASK_DATE_COLUMN = "task_date";
    public static final String TASK_PRIORITY_COLUMN = "task_priority";
    public static final String TASK_STATUS_COLUMN = "task_status";
    public static final String TASK_TIME_STAMP_COLUMN = "task_time_stamp";

    //    Новые колонки для данных дней недели
    public static final String TASK_DAY_COLUMN = "task_selected_day";
    public static final String TASK_TIME_DAY_COLUMN = "task_time";

    //    Константа для создания таблицы
    // константа из первой версии базы
//    private static final String TASKS_TABLE_CREATE_SCRIPT = "CREATE TABLE "
//            + TASK_TABLE + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//            + TASK_TITLE_COLUMN + " TEXT NOT NULL, "
//            + TASK_DATE_COLUMN + " LONG, "
//            + TASK_PRIORITY_COLUMN + " INTEGER, "
//            + TASK_STATUS_COLUMN + " INTEGER, "
//            + TASK_TIME_STAMP_COLUMN + " LONG); ";

    //    Версия нового запроса, после обновления версии базы данных:
    private static final String TASKS_NEW_TABLE_CREATE_SCRIPT = "CREATE TABLE "
            + TASK_TABLE
            + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TASK_TITLE_COLUMN + " TEXT NOT NULL, "
            + TASK_DATE_COLUMN + " LONG, "
            + TASK_PRIORITY_COLUMN + " INTEGER, "
            + TASK_STATUS_COLUMN + " INTEGER, "
            + TASK_TIME_STAMP_COLUMN + " LONG, "
            + TASK_DAY_COLUMN + " TEXT, "
            + TASK_TIME_DAY_COLUMN + " LONG); ";

    //    Константы для выборки значений
    public static final String SELECTION_STATUS = DBHelper.TASK_STATUS_COLUMN + " = ?";
    public static final String SELECTION_TIME_STAMP = TASK_TIME_STAMP_COLUMN + " = ?";
    public static final String SELECTION_LIKE_TITLE = TASK_TITLE_COLUMN + " LIKE ?";

//    public static final String SELECTION_TIME_FOR_DAY = TASK_TIME_DAY_COLUMN + " LIKE ?";

    //    для получения доступа из фрагментов
    private DBQueryManager queryManager;
    private DBUpdateManager updateManager;

    //    Конструктор Базы данных
    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

//        Инициализируем менеджеры
        queryManager = new DBQueryManager(getReadableDatabase());//для чтения из базы
        updateManager = new DBUpdateManager(getWritableDatabase());//для записи в базу
    }

    //    Создаем таблицу в базе
    @Override
    public void onCreate(SQLiteDatabase db) {
//        db.execSQL(TASKS_TABLE_CREATE_SCRIPT);// схема для первой версии базы
        db.execSQL(TASKS_NEW_TABLE_CREATE_SCRIPT);//схема для новой версии базы
    }

    //    Обновляем, удаляя записи в базе
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

//        Изначальный вариант
//        db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
//        onCreate(db);//пересоздаем таблицу

//        Если версия Базы данных выше текущей, то добавим новую колонку для идентификации дней недели
//        СОздадим новую талицу с колонками _id, название дней, часы, минуты, время
        if (newVersion > 1) {

            db.beginTransaction();
            try {
//                Скрипт создания временной таблицы
                String TASKS_TEMP_TABLE_CREATE_SCRIPT = "CREATE TEMPORARY TABLE "
                        + "temp_task_table "
                        + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + TASK_TITLE_COLUMN + " TEXT NOT NULL, "
                        + TASK_DATE_COLUMN + " LONG, "
                        + TASK_PRIORITY_COLUMN + " INTEGER, "
                        + TASK_STATUS_COLUMN + " INTEGER, "
                        + TASK_TIME_STAMP_COLUMN + " LONG); ";

//                Созадем временную таблицу
                db.execSQL(TASKS_TEMP_TABLE_CREATE_SCRIPT);
                System.out.println("=========================                                       Временная таблица создана");

//                Вставляем данные во временную таблицу из таблицы task_table
                db.execSQL("INSERT INTO temp_task_table (task_title, task_date, task_priority, task_status, task_time_stamp)" +
                        " SELECT task_title, task_date, task_priority, task_status, task_time_stamp FROM task_table ");
                System.out.println("=========================                                       Данные вставлены во временную таблицу");

//                Удаляем таблицу task_table если она существует
                db.execSQL("DROP TABLE IF EXISTS " + TASK_TABLE);
                System.out.println("=========================                                       Таблица изначальная УДАЛЕНА");

                onCreate(db);//создаем пустую таблицу для новой версии базы
                System.out.println("=========================                                       Пустая таблица создана");

//                Вставляем данные пользователя из временной таблицы в новую таблицу task_table
                db.execSQL("INSERT INTO task_table" +
                        " (task_title, task_date, task_priority, task_status, task_time_stamp)" +
                        " SELECT task_title, task_date, task_priority, task_status, task_time_stamp FROM temp_task_table ");
                System.out.println("=========================                                       Данные вставлены из временной таблицы в новую");

//                Удаляем временную талицу
                db.execSQL("DROP TABLE IF EXISTS temp_task_table ");
                System.out.println("=========================                                       Временная таблица УДАЛЕНА");

                db.setTransactionSuccessful();
                System.out.println("=========================                                       Транзакиця прошла успешно");
            } finally {
                db.endTransaction();
            }
        }
    }

    //    Сохраняем задачи
    public void saveTask(ModelTask task) {

//        Формируем данных для вставки
        ContentValues newValues = new ContentValues();
        newValues.put(TASK_TITLE_COLUMN, task.getTitle());
        newValues.put(TASK_DATE_COLUMN, task.getDate());
        newValues.put(TASK_STATUS_COLUMN, task.getStatus());
        newValues.put(TASK_PRIORITY_COLUMN, task.getPriority());
        newValues.put(TASK_TIME_STAMP_COLUMN, task.getTimeStamp());

        String dayTemp = "";

        for (int i = 0; i < task.getDay().length; i++) {
            dayTemp += task.getDay()[i];
        }

        newValues.put(TASK_DAY_COLUMN, dayTemp);
        newValues.put(TASK_TIME_DAY_COLUMN, task.getOnlyTime());

//        Вставляем данных в таблицу
        getWritableDatabase().insert(TASK_TABLE, null, newValues);
    }

    //   Методы для получения доступа к менеджерам из фрагментов
    public DBQueryManager query() {
        return queryManager;
    }

    public DBUpdateManager update() {
        return updateManager;
    }

    //    Добавляем в базу данных запрос на удаление задач
    public void removeTask(long timeStamp) {
        getWritableDatabase().delete(TASK_TABLE, SELECTION_TIME_STAMP, new String[]{Long.toString(timeStamp)});
    }
}