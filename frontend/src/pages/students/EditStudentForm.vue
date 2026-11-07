<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:">
      <!-- Status is read-only here: it changes through UC-STU-deactivate-student, not through the edit. -->
      <el-tag :type="studentBeingEdited.enabled ? 'success' : 'danger'" data-cy="student-status">
        {{ studentBeingEdited.enabled ? 'Active' : 'Deactivated' }}
      </el-tag>
      <el-button
        v-if="studentBeingEdited.enabled"
        type="danger"
        plain
        size="small"
        style="margin-left: 12px"
        data-cy="deactivate-student"
        @click="deactivateExistingStudent()"
      >
        Deactivate
      </el-button>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import axios from 'axios'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog'])

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  // Call the API to update the user. The status is not sent: the server ignores it on an edit.
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}

/**
 * UC-STU-deactivate-student, steps 3 to 6. The confirmation states the consequences from the use case's
 * Associated Information (step 4). Cancel (5a) sends nothing. Errors are toasted by the shared request
 * interceptor, which also opens the Forbidden page on a 403 (6b, 6c), so nothing is shown twice here.
 * After a success or a 409 (6a, already deactivated) the student is reloaded, so the status shows what the
 * server now holds.
 */
async function deactivateExistingStudent() {
  const { id, firstName, lastName } = studentBeingEdited.value
  const fullName = `${firstName} ${lastName}`

  try {
    await ElMessageBox.confirm(
      `${fullName} will no longer be able to log in or submit weekly activity reports and peer evaluations, ` +
        'and teammates will no longer evaluate this student. The account, team membership, and all previously ' +
        'submitted work are kept, and the account can be reactivated later. This is not a deletion. ' +
        'Deactivate this student?',
      'Warning',
      { confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch {
    return // 5a: cancelled, nothing sent
  }

  try {
    await deactivateStudent(id as number)
    ElMessage.success(`${fullName} has been deactivated.`)
  } catch (error) {
    if (!(axios.isAxiosError(error) && error.response?.status === 409)) return
  }
  await loadStudent(id as number)
}
</script>

<style scoped lang="scss"></style>
