/*
 *  Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.raywenderlich.deliciousfood

import android.content.Intent
import com.mindorks.paracamera.Camera
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.OnCompleteListener

class MainActivity: AppCompatActivity() {

  var fbAuth = FirebaseAuth.getInstance()

  override fun onCreate(savedInstanceState: Bundle ? ) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    FirebaseApp.initializeApp(this)

    var btnLogin = findViewById < Button > (R.id.btnLogin)
    btnLogin.setOnClickListener {
      view ->
      signIn(view, "jane.doe@mail.com", "123456")
    }
  }

  fun signIn(view: View, email: String, password: String) {
    showMessage(view, "Authenticating...")

    fbAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, OnCompleteListener < AuthResult > {
      task ->
      if (task.isSuccessful) {
        var intent = Intent(this, LoggedInActivity::class.java)
        intent.putExtra("id", fbAuth.currentUser?.email)
        startActivity(intent)

      } else {
        showMessage(view, "Error: ${task.exception?.message}")
      }
    })

  }

  fun showMessage(view: View, message: String) {
    Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE).setAction("Action", null).show()
  }
}