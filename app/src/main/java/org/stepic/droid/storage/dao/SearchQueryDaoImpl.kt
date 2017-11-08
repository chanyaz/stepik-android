package org.stepic.droid.storage.dao

import android.content.ContentValues
import android.database.Cursor
import org.stepic.droid.model.SearchQuery
import org.stepic.droid.model.SearchQuerySource
import org.stepic.droid.storage.operations.CrudOperations
import org.stepic.droid.storage.structure.DbStructureSearchQuery
import javax.inject.Inject


class SearchQueryDaoImpl @Inject
constructor(crudOperations: CrudOperations) : DaoBase<SearchQuery>(crudOperations) {
    override fun getDbName(): String = DbStructureSearchQuery.SEARCH_QUERY

    override fun getDefaultPrimaryColumn(): String = DbStructureSearchQuery.Column.QUERY_TEXT

    override fun getDefaultPrimaryValue(persistentObject: SearchQuery): String = persistentObject.text.toLowerCase()

    override fun getContentValues(persistentObject: SearchQuery): ContentValues {
        val contentValues = ContentValues()

        contentValues.put(DbStructureSearchQuery.Column.QUERY_TEXT, persistentObject.text.toLowerCase()) // toLowerCase to avoid problems with case sensitive duplicates due to SQLite

        return contentValues
    }

    override fun parsePersistentObject(cursor: Cursor): SearchQuery =
            SearchQuery(
                    text = cursor.getString(cursor.getColumnIndex(DbStructureSearchQuery.Column.QUERY_TEXT)),
                    source = SearchQuerySource.DB
            )

}