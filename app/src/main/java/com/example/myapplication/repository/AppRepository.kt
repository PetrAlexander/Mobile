package com.example.myapplication.repository

import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import com.example.myapplication.Second352_2023Application
import com.example.myapplication.api.ServerAPI
import com.example.myapplication.data.Faculty
import com.example.myapplication.data.Group
import com.example.myapplication.data.Student
import com.example.myapplication.database.UniversityDatabase
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

class AppRepository private constructor() {
    var university: MutableLiveData<List<Faculty>> = MutableLiveData()
    var faculty: MutableLiveData<List<Group>> = MutableLiveData()
    var group: MutableLiveData<List<Student>> = MutableLiveData()

    companion object{
        private var INSTANCE: AppRepository? = null

        fun newInstance(){
            if (INSTANCE == null){
                INSTANCE = AppRepository()
            }
        }
        fun get(): AppRepository{
            return INSTANCE?:
            throw IllegalAccessException("Репозиторий не инициализирован")
            }
        }

    val db = Room.databaseBuilder(
        Second352_2023Application.applicationContext(),
        UniversityDatabase::class.java, "uniDB.db"
    ).build()

    val universityDao = db.getDao()

    suspend fun newFaculty (name: String){
        val faculty =Faculty(id=null,name=name)
        withContext(Dispatchers.IO){
            universityDao.insertNewFaculty(faculty)
            university.postValue(universityDao.loadUniversity())
        }
    }

    suspend fun deleteFaculty(faculty: Faculty) {
        withContext(Dispatchers.IO) {
            universityDao.deleteFaculty(faculty)
            university.postValue(universityDao.loadUniversity())
        }
    }

    suspend fun loadFaculty (){
        withContext(Dispatchers.IO){
            university.postValue(universityDao.loadUniversity())
        }
    }
    suspend fun getFacultyGroups (facultyID: Long){
        withContext(Dispatchers.IO){
            faculty.postValue(universityDao.loadFacultyGroup(facultyID))
        }
    }
    suspend fun getfaculty(facultyID: Long): Faculty?{
        var f : Faculty?=null
        val job= CoroutineScope(Dispatchers.IO).launch {
            f=universityDao.getFaculty(facultyID)
        }
        job.join()
        return f
    }

    suspend fun getGroupStudents(groupID: Long) /*:List<Student> */{
        withContext(Dispatchers.IO){
            group.postValue(universityDao.loadGroupStudents(groupID))
        }

//        var f : List<Student> = emptyList()
//        val job = CoroutineScope(Dispatchers.IO).launch{
//            f = universityDao.loadGroupStudents(groupID)
//        }
//        job.join()
//        return f
    }

    suspend fun newGroup(facultyID: Long, name: String) {
        val group = Group(id=null,name=name,facultyID=facultyID)
        withContext(Dispatchers.IO) {
            universityDao.insertNewGroup(group)
            getFacultyGroups(facultyID)
        }
    }

    suspend fun newStudent(student: Student, groupID: Long) {
        withContext(Dispatchers.IO) {
            universityDao.insertNewStudent(student)
            getGroupStudents(student.groupID!!)
        }
    }

    suspend fun getGroup(groupID: Long): Group? {
        var f : Group?=null
        val job= CoroutineScope(Dispatchers.IO).launch {
            f=universityDao.getGroup(groupID)
        }
        job.join()
        return f
    }

    suspend fun editStudent(student: Student) {
        withContext(Dispatchers.IO) {
//            universityDao.insertNewStudent(student)
//            var _student = universityDao.getStudent(student.id!!)
//            _student = student
            universityDao.updateStudent(student)
            getGroupStudents(student.groupID!!)
        }
    }

    suspend fun deleteStudent(student: Student) {
        withContext(Dispatchers.IO) {
            universityDao.deleteStudent(student)
            getGroupStudents(student.groupID!!)
        }
    }

    suspend fun editFaculty(s: String, faculty: Faculty) {
        withContext(Dispatchers.IO) {
            //universityDao.insertNewStudent(student)
            faculty.name = s
            universityDao.updateFaculty(faculty)
            university.postValue(universityDao.loadUniversity())
        }
    }

    suspend fun editGroup(facultyID: Long, s: String, group: Group) {
        withContext(Dispatchers.IO) {
            //universityDao.insertNewStudent(student)
            group.name = s
            universityDao.updateGroup(group)
            university.postValue(universityDao.loadUniversity())
            getFacultyGroups(facultyID)
        }
    }

    suspend fun deleteGroup(facultyID: Long, group: Group) {
        withContext(Dispatchers.IO) {
            universityDao.deleteGroup(group)
            getFacultyGroups(facultyID)
        }
    }


//    suspend fun loadGroup (){
//        withContext(Dispatchers.IO){
//            group.postValue(universityDao.loadStudent())
//        }
//    }

    //передача н.факультета, определение списка(пустой или с уже созданными), добавление в него, обновление общее
   /* fun newFaculty(name: String){
        val faculty = Faculty(name=name)
        val list: MutableList<Faculty> =
            if (university.value != null)
            {
                (university.value as ArrayList<Faculty>)
            }
        else
            ArrayList<Faculty>()
        list.add(faculty)
        university.postValue(list)
    }
    fun newGroup(facultyID: UUID, name: String){
        // равно `null`, то `return` -выход из функции и возврат `null` иначе `value` присваивается переменной `u`.
        val u = university.value?: return
        val faculty = u.find{it. id== facultyID} ?: return
        val group = Group(name=name)
        val list: ArrayList<Group> =
            if (faculty.groups.isEmpty())
            ArrayList()
            else
                faculty.groups as ArrayList<Group>
        list.add(group)
        faculty.groups=list
        university.postValue(u)
    }
    //`faculty` будет хранить первый элемент из `u`, который имеет `groups`, содержащие элемент с
    // `id` равным `groupID`. Если таких элементов нет, то `faculty` будет равна `null`
    fun newStudent(groupID: UUID, student: Student){
        val u = university.value?: return
        val faculty = u.find { it.groups.find { it.id == groupID } != null } ?: return
        val group = faculty.groups.find { it.id == groupID }
        val list: ArrayList<Student> = if (group!!.students.isEmpty())
            ArrayList()
        else
            group.students as ArrayList<Student>
        list.add(student)
        group.students = list
        university.postValue(u)
    }
    fun deleteStudent(groupID: UUID, student: Student){
        val u = university.value?: return
        val faculty = u.find { it.groups.find { it.id == groupID } != null } ?: return
        val group = faculty.groups.find { it.id == groupID }
        if (group!!.students.isEmpty()) return
        val list = group.students as ArrayList<Student>
        list.remove(student)
        group.students = list
        university.postValue(u)
    }
    fun editStudent(groupID: UUID, student: Student){
        val u = university.value?: return
        val faculty = u.find { it.groups.find { it.id == groupID } != null } ?: return
        val group = faculty.groups.find { it.id == groupID } ?: return
        val _student = group.students.find { it.id==student.id }
        if (_student == null) {
            newStudent(groupID,student)
            return
        }
        val list = group.students as ArrayList<Student>
        val i=list.indexOf(_student)
        list.remove(_student)
        list.add(i,student)
        group.students = list
        university.postValue(u)
    }*/

    private var myServerAPI : ServerAPI? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getAPI(){
        val url = ""
        Retrofit.Builder()
            .baseUrl("http://${url}")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build().apply {
                myServerAPI = create(ServerAPI::class.java)
            }
    }

    fun getServerFaculty(){
        if (myServerAPI != null) {
            CoroutineScope(Dispatchers.Main).launch {
                fetchFaculty()
            }
        }
    }

    private suspend fun fetchFaculty() {
        if (myServerAPI != null) {
            val job = CoroutineScope(Dispatchers.IO).launch {
                val r = myServerAPI!!.getFaculty().execute()
                if (r.isSuccessful){
                    val job = CoroutineScope(Dispatchers.IO).launch {
                        universityDao.deleteAllFaculty()
                    }
                    job.join()

                    val facultyList = r.body()
                    if (facultyList != null) {
                        for (f in facultyList) {
                            universityDao.insertNewFaculty(f)
                        }
                    }
                }
            }
            job.join()
            loadFaculty()
        }
    }
}