package com.example.quizapp.presentation

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.text.HtmlCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quizapp.domain.model.Question
import com.example.quizapp.domain.model.categoryMap
import com.example.quizapp.domain.repository.QuizRepository
import com.example.quizapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val repository: QuizRepository,
    private val savedStateHandle: SavedStateHandle //used for passing argument to viewmodel from navigation
) : ViewModel() {

    private val _state = mutableStateOf(QuizState())
    val state: State<QuizState> = _state

    init{
        loadQuestions()
    }

    fun loadQuestions(){
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val category = savedStateHandle.get<String>("category")
            val nQuestions = savedStateHandle.get<Int>("nQuestions") ?: return@launch //ellers brokker den sig med nullable
            var difficulty = savedStateHandle.get<String>("difficulty") ?: return@launch
            difficulty = difficulty.replaceFirstChar { it.lowercase() }
            if (difficulty == "all Difficulty"){
                difficulty = ""
            }
            val result = repository.getQuestionList(nQuestions, categoryMap[category]!!, difficulty)
            when(result){
                is Resource.Success -> {
                    val resultList = result.data!!.results.mapIndexed {index, entry ->
                        Question(entry.category, entry.correct_answer, entry.difficulty, entry.incorrect_answers, encodedTextConverter(entry.question), entry.type, 0)
                    }

                    _state.value = _state.value.copy(
                        questions = resultList,
                    )
                    shuffleAnswers(0)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
                is Resource.Error -> {
                    println("an error accured when fetching data from API")
                }
            }
        }
    }
    fun encodedTextConverter(text: String): String {
        return HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    fun shuffleAnswers(questionNumber: Int){
        val questions = _state.value.questions
        val listToShuffle: MutableList<String> = mutableListOf()
        for (answer in questions[questionNumber].incorrect_answers){
            listToShuffle.add(answer)
        }
        listToShuffle.add(questions[questionNumber].correct_answer)
        listToShuffle.shuffle()
        _state.value = _state.value.copy(
            shuffledAnswers = listToShuffle
        )
    }

    fun checkAnswer(answerOption: Int){
        val state = _state.value
        var correctAnswer: Int = 5 //dummy value
        for(i in 0..3){
            if(state.shuffledAnswers[i] == state.questions[state.currentQuestionNumber].correct_answer){
                correctAnswer = i
                break
            }
        }

        val answers: MutableList<String> = mutableListOf("", "", "", "")
        if (answerOption == correctAnswer){
            _state.value.questions[state.currentQuestionNumber].answerStatus = 1
            val rightAnswer = _state.value.nRightAnswers +1
            answers[answerOption] = "answerCorrect"
            _state.value = _state.value.copy(
                answerStatus = answers,
                nRightAnswers = rightAnswer
            )
        }else{
            _state.value.questions[state.currentQuestionNumber].answerStatus = 2
            answers[correctAnswer] = "answerCorrect"
            answers[answerOption] = "answerWrong"
            _state.value = _state.value.copy(
                answerStatus = answers
            )
        }

        //Set state ready for next screen and save the answer for later
        val saveAnswer = _state.value.shuffledAnswers[answerOption]
        val saveAnswerList = _state.value.saveAnswer
        saveAnswerList.add(saveAnswer)
        _state.value = _state.value.copy(
            pressToContinue = true,
            saveAnswer = saveAnswerList
        )
    }

    fun pressContinue(){
        val newQuestionNumber = _state.value.currentQuestionNumber + 1
        val answerOptions: List<String> = listOf("", "", "", "")

        //check if it was the last question
        if (newQuestionNumber == _state.value.questions.size){
            _state.value = _state.value.copy(
                gameOver = true
            )
            return
        }

        _state.value = _state.value.copy(
            currentQuestionNumber  = newQuestionNumber,
            answerStatus = answerOptions,
            pressToContinue = false
        )
        shuffleAnswers(_state.value.currentQuestionNumber)
    }

    /*
    fun loadQuestions(){
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = repository.getQuestionList(10, categoryMap["history"]!!, "easy")
            when(result){
                is Resource.Success -> {
                    val resultList = result.data!!.results.mapIndexed { index, entry ->
                        Question(entry.category, entry.correct_answer, entry.difficulty, entry.incorrect_answers, entry.question, entry.type)
                    }
                    _state.value = _state.value.copy(
                        questions = resultList,
                        isLoading = false,
                        error = null
                    )
                }
                is Resource.Error -> {
                }
            }

        }
    }
*/
}




